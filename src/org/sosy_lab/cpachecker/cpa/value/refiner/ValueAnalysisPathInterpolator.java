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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.AssumptionUseDefinitionCollector;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier.ErrorPathPrefixPreference;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.InitialAssumptionUseDefinitionCollector;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisEdgeInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.value.refiner")
public class ValueAnalysisPathInterpolator implements Statistics {
  /**
   * whether or not to do lazy-abstraction, i.e., when true, the re-starting node
   * for the re-exploration of the ARG will be the node closest to the root
   * where new information is made available through the current refinement
   */
  @Option(secure=true, description="whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  @Option(secure=true, description="whether to perform (more precise) edge-based interpolation or (more efficient) path-based interpolation")
  private boolean performEdgeBasedInterpolation = true;

  @Option(secure=true, description="which prefix of an actual counterexample trace should be used for interpolation")
  private ErrorPathPrefixPreference prefixPreference = ErrorPathPrefixPreference.DOMAIN_BEST_SHALLOW;

  /**
   * the offset in the path from where to cut-off the subtree, and restart the analysis
   */
  private int interpolationOffset = -1;

  /**
   * a reference to the assignment-counting state, to make the precision increment aware of thresholds
   */
  private UniqueAssignmentsInPathConditionState assignments = null;

  // statistics
  private StatCounter totalInterpolations   = new StatCounter("Number of interpolations");
  private StatInt totalInterpolationQueries = new StatInt(StatKind.SUM, "Number of interpolation queries");
  private StatInt sizeOfInterpolant         = new StatInt(StatKind.AVG, "Size of interpolant");
  private StatTimer timerInterpolation      = new StatTimer("Time for interpolation");

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;

  private final ValueAnalysisEdgeInterpolator interpolator;

  public ValueAnalysisPathInterpolator(Configuration pConfig,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;

    logger           = pLogger;
    cfa              = pCfa;
    shutdownNotifier = pShutdownNotifier;
    interpolator     = new ValueAnalysisEdgeInterpolator(pConfig, logger, shutdownNotifier, cfa);
  }

  protected Map<ARGState, ValueAnalysisInterpolant> performInterpolation(ARGPath errorPath,
      ValueAnalysisInterpolant interpolant) throws CPAException, InterruptedException {
    totalInterpolations.inc();
    timerInterpolation.start();

    interpolationOffset = -1;

    ARGPath errorPathPrefix = obtainErrorPathPrefix(errorPath, interpolant);

    Map<ARGState, ValueAnalysisInterpolant> pathInterpolants = (performEdgeBasedInterpolation)
        ? performEdgeBasedInterpolation(errorPathPrefix, interpolant)
        : performPathBasedInterpolation(errorPathPrefix);

    timerInterpolation.stop();
    return pathInterpolants;
  }

  /**
   * This method performs interpolation on each edge of the path, using the
   * {@link ValueAnalysisEdgeInterpolator}.
   *
   * @param errorPathPrefix the error path prefix to interpolate
   * @param interpolant an initial interpolant (only non-trivial when interpolating error path suffixes in global refinement)
   * @return the mapping of {@link ARGState}s to {@link ValueAnalysisInterpolant}s
   * @throws InterruptedException
   * @throws CPAException
   */
  private Map<ARGState, ValueAnalysisInterpolant> performEdgeBasedInterpolation(ARGPath errorPathPrefix,
      ValueAnalysisInterpolant interpolant)
      throws InterruptedException, CPAException {

    // obtain use-def relation, containing variables relevant to the "failing" assumption
    Set<MemoryLocation> useDefRelation = new HashSet<>();
    /* TODO: does not work as long as AssumptionUseDefinitionCollector is incomplete (e.g., does not take structs into account)
    if (prefixPreference != ErrorPathPrefixPreference.DEFAULT) {
      AssumptionUseDefinitionCollector useDefinitionCollector = new InitialAssumptionUseDefinitionCollector();
      useDefRelation = from(useDefinitionCollector.obtainUseDefInformation(errorTrace)).
          transform(MemoryLocation.FROM_STRING_TO_MEMORYLOCATION).toSet();
    }*/

    Map<ARGState, ValueAnalysisInterpolant> pathInterpolants = new LinkedHashMap<>(errorPathPrefix.size());

    PathIterator pathIterator = errorPathPrefix.pathIterator();
    while(pathIterator.hasNext()) {
      shutdownNotifier.shutdownIfNecessary();

      // interpolate at each edge as long the previous interpolant is not false
      if (!interpolant.isFalse()) {
        interpolant = interpolator.deriveInterpolant(errorPathPrefix,
            pathIterator.getOutgoingEdge(),
            pathIterator.getIndex(),
            interpolant,
            useDefRelation);
      }

      totalInterpolationQueries.setNextValue(interpolator.getNumberOfInterpolationQueries());

      if (!interpolant.isTrivial() && interpolationOffset == -1) {
        interpolationOffset = pathIterator.getIndex();
      }

      sizeOfInterpolant.setNextValue(interpolant.getSize());

      pathIterator.advance();

      pathInterpolants.put(pathIterator.getAbstractState(), interpolant);

      if(!pathIterator.hasNext()) {
        assert interpolant.isFalse() : "final interpolant is not false";
      }
    }

    return pathInterpolants;
  }

  /**
   * This method performs interpolation on the complete path, based on the
   * use-def-relation obtained by {@link AssumptionUseDefinitionCollector} or
   * its subclass. It creates fake interpolants that are not inductive.
   *
   * @param errorPathPrefix the error path prefix to interpolate
   * @return
   */
  private Map<ARGState, ValueAnalysisInterpolant> performPathBasedInterpolation(ARGPath errorPathPrefix) {

    assert(errorPathPrefix.getFirstState().getParents().isEmpty())
    : "static interpolation requires cpa.value.refinement.useTopDownInterpolationStrategy to be set to 'true'";

    AssumptionUseDefinitionCollector useDefinitionCollector = prefixPreference == ErrorPathPrefixPreference.DEFAULT ?
        new AssumptionUseDefinitionCollector() :
        new InitialAssumptionUseDefinitionCollector();

    Set<String> useDefRelation = useDefinitionCollector.obtainUseDefInformation(errorPathPrefix.asEdgesList());

    totalInterpolationQueries.setNextValue(1);
    sizeOfInterpolant.setNextValue(useDefRelation.size() * errorPathPrefix.size());

    Map<ARGState, ValueAnalysisInterpolant> pathInterpolants = new LinkedHashMap<>(errorPathPrefix.size());

    // add the "fake" interpolant for each state except the root of the ARG;
    // this makes the first child of the root the refinement root
    ValueAnalysisInterpolant fakeItp = createFakeInterpolant(useDefRelation);
    for(ARGState state : Iterables.skip(errorPathPrefix.asStatesList(), 1)) {
      pathInterpolants.put(state, fakeItp);
    }

    return pathInterpolants;
  }

  /**
   * This method creates a "fake" interpolant from a set of relevant variables.
   *
   * @param relevantVariables
   * @return the "fake" interpolant
   */
  private ValueAnalysisInterpolant createFakeInterpolant(Set<String> relevantVariables) {
    HashMap<MemoryLocation, Value> values = new HashMap<>();
    for (String relevantVariable : relevantVariables) {
      values.put(MemoryLocation.valueOf(relevantVariable), UnknownValue.getInstance());
    }

    return new ValueAnalysisInterpolant(values, Collections.<MemoryLocation, Type>emptyMap());
  }

  public Multimap<CFANode, MemoryLocation> determinePrecisionIncrement(MutableARGPath errorPath)
      throws CPAException, InterruptedException {

    assignments = AbstractStates.extractStateByType(errorPath.getLast().getFirst(),
        UniqueAssignmentsInPathConditionState.class);

    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

    Map<ARGState, ValueAnalysisInterpolant> itps = performInterpolation(errorPath.immutableCopy(),
        ValueAnalysisInterpolant.createInitial());

    for (Map.Entry<ARGState, ValueAnalysisInterpolant> itp : itps.entrySet()) {
      addToPrecisionIncrement(increment, AbstractStates.extractLocation(itp.getKey()), itp.getValue());
    }

    return increment;
  }

  /**
   * This method adds the given variable at the given location to the increment.
   *
   * @param increment the current increment
   * @param currentNode the current node for which to add a new variable
   * @param memoryLocation the name of the variable to add to the increment at the given edge
   */
  private void addToPrecisionIncrement(Multimap<CFANode, MemoryLocation> increment,
      CFANode currentNode,
      ValueAnalysisInterpolant itp) {
    for (MemoryLocation memoryLocation : itp.getMemoryLocations()) {
      if (assignments == null || !assignments.exceedsHardThreshold(memoryLocation)) {
        increment.put(currentNode, memoryLocation);
      }
    }
  }

  /**
   * This method determines the new refinement root.
   *
   * @param errorPath the error path from where to determine the refinement root
   * @param increment the current precision increment
   * @param isRepeatedRefinement the flag to determine whether or not this is a repeated refinement
   * @return the new refinement root
   * @throws RefinementFailedException if no refinement root can be determined
   */
  public Pair<ARGState, CFAEdge> determineRefinementRoot(MutableARGPath errorPath, Multimap<CFANode, MemoryLocation> increment,
      boolean isRepeatedRefinement) throws RefinementFailedException {

    if (interpolationOffset == -1) {
      throw new RefinementFailedException(Reason.InterpolationFailed, errorPath.immutableCopy());
    }

    // if doing lazy abstraction, use the node closest to the root node where new information is present
    if (doLazyAbstraction) {
      return errorPath.get(interpolationOffset);
    }

    // otherwise, just use the successor of the root node
    else {
      return errorPath.get(1);
    }
  }

  /**
   * This path obtains a (sub)path of the error path which is given to the interpolation procedure.
   *
   * @param errorPath the original error path
   * @param interpolant the initial interpolant, i.e. the initial state, with which to check the error path.
   * @return a (sub)path of the error path which is given to the interpolation procedure
   * @throws CPAException
   * @throws InterruptedException
   */
  private ARGPath obtainErrorPathPrefix(ARGPath errorPath, ValueAnalysisInterpolant interpolant)
          throws CPAException, InterruptedException {

    try {
      ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa, config);
      List<ARGPath> prefixes = checker.getInfeasilbePrefixes(errorPath, interpolant.createValueAnalysisState());

      ErrorPathClassifier classifier = new ErrorPathClassifier(cfa.getVarClassification(), cfa.getLoopStructure());
      errorPath = classifier.obtainPrefix(prefixPreference, errorPath, prefixes);

    } catch (InvalidConfigurationException e) {
      throw new CPAException("Configuring ValueAnalysisFeasibilityChecker failed: " + e.getMessage(), e);
    }

    return errorPath;
  }

  @Override
  public String getName() {
    return "ValueAnalysisInterpolationBasedRefiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out).beginLevel();
    writer.put(totalInterpolations);
    writer.put(totalInterpolationQueries);
    writer.put(sizeOfInterpolant);
    writer.put(timerInterpolation);
  }

  public int getInterpolationOffset() {
    return interpolationOffset;
  }
}
