/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * This class can check feasibility of a simple path using an SMT solver.
 */
public class PathChecker {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final PathFormulaManager pmgr;
  private final Solver solver;
  private final MachineModel machineModel;

  public PathChecker(LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      PathFormulaManager pPmgr, Solver pSolver,
      MachineModel pMachineModel) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    pmgr = pPmgr;
    solver = pSolver;
    machineModel = pMachineModel;
  }

  public CounterexampleTraceInfo checkPath(List<CFAEdge> pPath)
      throws SolverException, CPATransferException, InterruptedException {

    Pair<PathFormula, List<SSAMap>> result = createPrecisePathFormula(pPath);

    List<SSAMap> ssaMaps = result.getSecond();

    PathFormula pathFormula = result.getFirst();

    BooleanFormula f = pathFormula.getFormula();

    try (ProverEnvironment thmProver = solver.newProverEnvironmentWithModelGeneration()) {
      thmProver.push(f);
      if (thmProver.isUnsat()) {
        return CounterexampleTraceInfo.infeasibleNoItp();
      } else {
        Model model = getModel(thmProver);

        Pair<CFAPathWithAssignments, Multimap<CFAEdge, AssignableTerm>> pathAndTerms = extractVariableAssignment(pPath, ssaMaps, model);

        CFAPathWithAssignments pathWithAssignments = pathAndTerms.getFirst();
        Multimap<CFAEdge, AssignableTerm> termsPerEdge = pathAndTerms.getSecond();

        model = model.withAssignmentInformation(pathWithAssignments, termsPerEdge);

        return CounterexampleTraceInfo.feasible(ImmutableList.of(f), model, ImmutableMap.<Integer, Boolean>of());
      }
    }
  }

  private Pair<PathFormula, List<SSAMap>> createPrecisePathFormula(List<CFAEdge> pPath)
      throws CPATransferException, InterruptedException {

    List<SSAMap> ssaMaps = new ArrayList<>(pPath.size());

    PathFormula pathFormula = pmgr.makeEmptyPathFormula();

    for (CFAEdge edge : from(pPath).filter(notNull())) {

      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
        for (CFAEdge singleEdge : (MultiEdge) edge) {
          pathFormula = pmgr.makeAnd(pathFormula, singleEdge);
          ssaMaps.add(pathFormula.getSsa());
        }
      } else {
        pathFormula = pmgr.makeAnd(pathFormula, edge);
        ssaMaps.add(pathFormula.getSsa());
      }
    }

    return Pair.of(pathFormula, ssaMaps);
  }

  /**
   * Calculate the precise SSAMaps for the given path.
   * Multi-edges will be resolved. The resulting list of SSAMaps
   * need not be the same size as the given path.
   *
   * @param pPath calculate the precise list of SSAMaps for this path.
   * @return the precise list of SSAMaps for the given path.
   * @throws CPATransferException
   * @throws InterruptedException
   */
  public List<SSAMap> calculatePreciseSSAMaps(List<CFAEdge> pPath)
      throws CPATransferException, InterruptedException {

    return createPrecisePathFormula(pPath).getSecond();
  }

  public Pair<CFAPathWithAssignments, Multimap<CFAEdge, AssignableTerm>> extractVariableAssignment(List<CFAEdge> pPath,
      List<SSAMap> pSsaMaps, Model pModel) throws InterruptedException {

    AssignmentToPathAllocator allocator = new AssignmentToPathAllocator(logger, shutdownNotifier);

    return allocator.allocateAssignmentsToPath(pPath, pModel, pSsaMaps, machineModel);
  }

  private Model getModel(ProverEnvironment thmProver) {
    try {
      return thmProver.getModel();
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Solver could not produce model, variable assignment of error path can not be dumped.");
      logger.logDebugException(e);
      return Model.empty();
    }
  }
}