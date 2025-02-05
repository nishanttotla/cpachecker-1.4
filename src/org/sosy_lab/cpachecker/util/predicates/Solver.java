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

import java.util.Map;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.OptEnvironmentView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.SeparateInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.logging.LoggingInterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.logging.LoggingOptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.logging.LoggingProverEnvironment;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

/**
 * Encapsulation of an SMT solver.
 * This class is the central entry point to everything related to an SMT solver:
 * formula creation and manipulation (via the {@link #getFormulaManager()} method),
 * and checking for satisfiability (via the remaining methods).
 * In addition to the low-level methods provided by {@link FormulaManager},
 * this class and {@link FormulaManagerView} provide additional higher-level utility methods,
 * and additional features such as
 * replacing one SMT theory transparently with another,
 * or using different SMT solvers for different tasks such as solving and interpolation.
 */
@Options(prefix="cpa.predicate")
public final class Solver {

  @Option(secure=true, name="solver.useLogger",
      description="log some solver actions, this may be slow!")
  private boolean useLogger = false;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  private final FormulaManager solvingFormulaManager;
  private final FormulaManager interpolationFormulaManager;

  private final Map<BooleanFormula, Boolean> unsatCache = Maps.newHashMap();

  private final LogManager logger;

  // stats
  public final Timer solverTime = new Timer();
  public int satChecks = 0;
  public int trivialSatChecks = 0;
  public int cachedSatChecks = 0;

  /**
   * Please use {@link #create(Configuration, LogManager, ShutdownNotifier)} in normal code.
   * This constructor is primarily for test code.
   */
  @VisibleForTesting
  public Solver(FormulaManagerView pFmgr, FormulaManagerFactory pFactory,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    logger = pLogger;
    solvingFormulaManager = pFactory.getFormulaManager();
    interpolationFormulaManager = pFactory.getFormulaManagerForInterpolation();
  }

  /**
   * Load and instantiate an SMT solver.
   */
  public static Solver create(Configuration config, LogManager logger,
      ShutdownNotifier shutdownNotifier) throws InvalidConfigurationException {
    FormulaManagerFactory factory = new FormulaManagerFactory(config, logger, shutdownNotifier);
    FormulaManagerView fmgr = new FormulaManagerView(factory, config, logger);
    return new Solver(fmgr, factory, config, logger);
  }

  /**
   * Return the underlying {@link FormulaManagerView}
   * that can be used for creating and manipulating formulas.
   */
  public FormulaManagerView getFormulaManager() {
    return fmgr;
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link ProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public ProverEnvironment newProverEnvironment() {
    return newProverEnvironment(false, false);
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link ProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   *
   * The solver is told to enable model generation.
   */
  public ProverEnvironment newProverEnvironmentWithModelGeneration() {
    return newProverEnvironment(true, false);
  }

  /**
   * Direct reference to the underlying SMT solver for more complicated queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link ProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   *
   * The solver is told to enable unsat-core generation.
   */
  public ProverEnvironment newProverEnvironmentWithUnsatCoreGeneration() {
    return newProverEnvironment(false, true);
  }

  private ProverEnvironment newProverEnvironment(boolean generateModels, boolean generateUnsatCore) {
    ProverEnvironment pe = solvingFormulaManager.newProverEnvironment(generateModels, generateUnsatCore);

    if (useLogger) {
      return new LoggingProverEnvironment(logger, pe);
    } else {
      return pe;
    }
  }

  /**
   * Direct reference to the underlying SMT solver for interpolation queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link InterpolatingProverEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation() {
    InterpolatingProverEnvironment<?> ipe = interpolationFormulaManager.newProverEnvironmentWithInterpolation(false);

    if (solvingFormulaManager != interpolationFormulaManager) {
      // If interpolationFormulaManager is not the normal solver,
      // we use SeparateInterpolatingProverEnvironment
      // which copies formula back and forth using strings.
      // We don't need this if the solvers are the same anyway.
      ipe = new SeparateInterpolatingProverEnvironment<>(solvingFormulaManager, interpolationFormulaManager, ipe);
    }

    if (useLogger) {
      return new LoggingInterpolatingProverEnvironment<>(logger, ipe);
    } else {
      return ipe;
    }
  }

  /**
   * Direct reference to the underlying SMT solver for optimization queries.
   * This creates a fresh, new, environment in the solver.
   * This environment needs to be closed after it is used by calling {@link OptEnvironment#close()}.
   * It is recommended to use the try-with-resources syntax.
   */
  public OptEnvironment newOptEnvironment() {
    OptEnvironment environment = solvingFormulaManager.newOptEnvironment();
    environment = new OptEnvironmentView(environment, fmgr);

    if (useLogger) {
      return new LoggingOptEnvironment(logger, environment);
    } else {
      return environment;
    }
  }

  /**
   * Checks whether a formula is unsat.
   */
  public boolean isUnsat(BooleanFormula f) throws SolverException, InterruptedException {
    satChecks++;

    if (bfmgr.isTrue(f)) {
      trivialSatChecks++;
      return false;
    }
    if (bfmgr.isFalse(f)) {
      trivialSatChecks++;
      return true;
    }
    Boolean result = unsatCache.get(f);
    if (result != null) {
      cachedSatChecks++;
      return result;
    }

    solverTime.start();
    try {
      result = isUnsatUncached(f);

      unsatCache.put(f, result);
      return result;

    } finally {
      solverTime.stop();
    }
  }

  private boolean isUnsatUncached(BooleanFormula f) throws SolverException, InterruptedException {
    try (ProverEnvironment prover = newProverEnvironment()) {
      prover.push(f);
      return prover.isUnsat();
    }
  }

  /**
   * Checks whether a => b.
   * The result is cached.
   */
  public boolean implies(BooleanFormula a, BooleanFormula b) throws SolverException, InterruptedException {
    if (bfmgr.isFalse(a) || bfmgr.isTrue(b)) {
      satChecks++;
      trivialSatChecks++;
      return true;
    }
    if (a.equals(b)) {
      satChecks++;
      trivialSatChecks++;
      return true;
    }

    BooleanFormula f = bfmgr.not(bfmgr.implication(a, b));

    return isUnsat(f);
  }

  /**
   * Populate the cache for unsatisfiability queries with a formula
   * that is known to be unsat.
   * @param unsat An unsatisfiable formula.
   */
  public void addUnsatisfiableFormulaToCache(BooleanFormula unsat) {
    if (unsatCache.containsKey(unsat) || bfmgr.isFalse(unsat)) {
      return;
    }
    try {
      assert isUnsatUncached(unsat) : "formula is sat: " + unsat;
    } catch (SolverException e) {
      throw new AssertionError(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    unsatCache.put(unsat, true);
  }
}
