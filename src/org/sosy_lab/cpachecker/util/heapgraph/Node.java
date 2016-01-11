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
package org.sosy_lab.cpachecker.util.heapgraph;

import org.sosy_lab.cpachecker.util.heapgraph.Graph.ThreeVal;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

// Define node for a heap graph
public class Node {
  public BooleanFormula predicate; // TODO this predicate should ideally be uniform across nodes
  public ThreeVal predLabel;

  public boolean isRoot;
  public boolean summary; // TODO these properties aren't being updated right now

  public Node(BooleanFormula predicate) {
    this.predicate = predicate;
    this.summary = false;
    this.predLabel = ThreeVal.MAYBE;
  }

  public Node(BooleanFormula predicate, boolean isRoot) {
    this.predicate = predicate;
    this.isRoot = isRoot;
    this.summary = false;
    this.predLabel = ThreeVal.MAYBE;
  }

  public void setAsSummary() {
    this.summary = true;
  }

  public void setAsNotSummary() {
    this.summary = false;
  }
}