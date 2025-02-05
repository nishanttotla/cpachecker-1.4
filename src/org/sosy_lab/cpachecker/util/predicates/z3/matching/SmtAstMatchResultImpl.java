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
package org.sosy_lab.cpachecker.util.predicates.z3.matching;

import java.util.Collection;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class SmtAstMatchResultImpl implements SmtAstMatchResult {

  private final Multimap<SmtAstPattern, Formula> argumentPatternMatches;
  private final Multimap<String, Formula> variableBindings;
  private Formula matchingRootFormula;

  public SmtAstMatchResultImpl() {
    this.argumentPatternMatches = HashMultimap.create();
    this.variableBindings = HashMultimap.create();
  }

  public void putMatchingArgumentFormula(SmtAstPattern pArgumentPattern, Formula pMatchingFormula) {
    argumentPatternMatches.put(pArgumentPattern, pMatchingFormula);
  }

  public void setMatchingRootFormula(Formula pMatchingFormula) {
    matchingRootFormula = pMatchingFormula;
  }

  public void putBoundVaribale(String pVariable, Formula pBoundFormula) {
    variableBindings.put(pVariable, pBoundFormula);
  }

  @Override
  public Collection<Formula> getMatchingArgumentFormula(SmtAstPattern pArgumentPattern) {
    return argumentPatternMatches.get(pArgumentPattern);
  }

  @Override
  public Optional<Formula> getMatchingRootFormula() {
    Preconditions.checkNotNull(matchingRootFormula);
    return Optional.of(matchingRootFormula);
  }

  @Override
  public Collection<Formula> getVariableBindings(String pString) {
    return variableBindings.get(pString);
  }

  @Override
  public boolean matches() {
    Preconditions.checkNotNull(matchingRootFormula);
    return true;
  }

  @Override
  public Collection<String> getBoundVariables() {
    return variableBindings.keySet();
  }

  @Override
  public String toString() {
    return String.format("VarBindings: %s", variableBindings.toString());
  }

}
