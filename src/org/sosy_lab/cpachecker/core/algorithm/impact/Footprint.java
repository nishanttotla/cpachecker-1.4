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
package org.sosy_lab.cpachecker.core.algorithm.impact;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

public class Footprint {
  Set<BooleanFormula> terms;
  Set<String> heapVarsSeen; // all heapvars seen so far, need to be tracked in the graph

  Footprint() {
    terms = new HashSet<>();
    heapVarsSeen = new HashSet<>();
  }

  public Footprint(Footprint pre) {
    this();
    terms.addAll(pre.terms);
    heapVarsSeen.addAll(pre.heapVarsSeen);
  }

  public Footprint(Footprint pre, BooleanFormula term) {
    this(pre);
    terms.add(term);
  }

  public Footprint(Footprint pre, String heapVar) {
    this(pre);
    heapVarsSeen.add(heapVar);
  }

  public Set<BooleanFormula> getTerms() {
    return terms;
  }

  public Set<String> getHeapVars() {
    return heapVarsSeen;
  }
}