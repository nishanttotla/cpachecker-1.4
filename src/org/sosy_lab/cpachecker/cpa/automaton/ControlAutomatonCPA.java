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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.Optional;
import org.sosy_lab.cpachecker.core.defaults.BreakOnTargetsPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

/**
 * This class implements an AutomatonAnalysis as described in the related Documentation.
 */
@Options(prefix="cpa.automaton")
public class ControlAutomatonCPA implements ConfigurableProgramAnalysis, StatisticsProvider, ConfigurableProgramAnalysisWithBAM, ProofChecker {

  @Option(secure=true, name="dotExport",
      description="export automaton to file")
  private boolean export = false;

  @Option(secure=true, name="dotExportFile",
      description="file for saving the automaton in DOT format (%s will be replaced with automaton name)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportFile = PathTemplate.ofFormatString("%s.dot");

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ControlAutomatonCPA.class);
  }

  @Option(secure=true, required=false,
      description="file with automaton specification for ObserverAutomatonCPA and ControlAutomatonCPA")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path inputFile = null;

  @Option(secure=true, description="signal the analysis to break in case the given number of error state is reached ")
  private int breakOnTargetState = 1;

  @Option(secure=true, description="the maximum number of iterations performed after the initial error is found, despite the limit"
      + "given as cpa.automaton.breakOnTargetState is not yet reached")
  private int extraIterationsLimit = -1;

  private final Automaton automaton;
  private final AutomatonState topState = new AutomatonState.TOP(this);
  private final AutomatonState bottomState = new AutomatonState.BOTTOM(this);

  private final AbstractDomain automatonDomain = new FlatLatticeDomain(topState);
  private final StopOperator stopOperator = new StopSepOperator(automatonDomain);
  private final AutomatonTransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final Statistics stats = new AutomatonStatistics(this);

  private final CFA cfa;
  private final LogManager logger;

  protected ControlAutomatonCPA(@Optional Automaton pAutomaton, Configuration pConfig, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {

    pConfig.inject(this, ControlAutomatonCPA.class);

    this.cfa = pCFA;
    this.logger = pLogger;

    transferRelation = new AutomatonTransferRelation(this, pConfig, pLogger);

    final PrecisionAdjustment lPrecisionAdjustment;

    if (breakOnTargetState > 0) {
      lPrecisionAdjustment = BreakOnTargetsPrecisionAdjustment.getInstance(breakOnTargetState, extraIterationsLimit);

    } else {
      lPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
    }

    precisionAdjustment = new PrecisionAdjustment() { // Handle the BREAK state

      @Override
      public PrecisionAdjustmentResult prec(AbstractState pState, Precision pPrecision,
          UnmodifiableReachedSet pStates, AbstractState fullState) throws CPAException, InterruptedException {

        PrecisionAdjustmentResult wrappedPrec = lPrecisionAdjustment.prec(pState, pPrecision, pStates, fullState);

        if (((AutomatonState) pState).getInternalStateName().equals("_predefinedState_BREAK")) {
          return wrappedPrec.withAction(Action.BREAK);
        }

        return wrappedPrec;
      }
    };

    if (pAutomaton != null) {
      this.automaton = pAutomaton;

    } else if (inputFile == null) {
      throw new InvalidConfigurationException("Explicitly specified automaton CPA needs option cpa.automaton.inputFile!");

    } else {

      Language language = pCFA.getLanguage();

      List<Automaton> lst;

      Scope scope;

      switch (language) {
      case C:
        scope = new CProgramScope(pCFA, pLogger);
        break;
      default:
        scope = DummyScope.getInstance();
        break;
      }

      lst = AutomatonParser.parseAutomatonFile(inputFile, pConfig, pLogger, pCFA.getMachineModel(), scope, language);

      if (lst.isEmpty()) {
        throw new InvalidConfigurationException("Could not find automata in the file " + inputFile.toAbsolutePath());
      } else if (lst.size() > 1) {
        throw new InvalidConfigurationException("Found " + lst.size()
            + " automata in the File " + inputFile.toAbsolutePath()
            + " The CPA can only handle ONE Automaton!");
      }

      this.automaton = lst.get(0);
    }
    pLogger.log(Level.FINEST, "Automaton", automaton.getName(), "loaded.");

    if (export && exportFile != null) {
      try (Writer w = Files.openOutputFile(exportFile.getPath(automaton.getName()))) {
        automaton.writeDotFile(w);
      } catch (IOException e) {
        pLogger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
    }

    GlobalInfo.getInstance().storeAutomaton(automaton);
  }

  Automaton getAutomaton() {
    return this.automaton;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return automatonDomain;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return AutomatonState.automatonStateFactory(automaton.getInitialVariables(), automaton.getInitialState(), this, 0, 0, null);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public AutomatonTransferRelation getTransferRelation() {
    return transferRelation ;
  }

  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }

  public AutomatonState getBottomState() {
    return this.bottomState;
  }

  public AutomatonState getTopState() {
    return this.topState;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    return pSuccessors.equals(getTransferRelation().getAbstractSuccessorsForEdge(
        pElement, SingletonPrecision.getInstance(), pCfaEdge));
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement) throws CPAException, InterruptedException {
    return getAbstractDomain().isLessOrEqual(pElement, pOtherElement);
  }

  MachineModel getMachineModel() {
    return cfa.getMachineModel();
  }

  LogManager getLogManager() {
    return logger;
  }
}
