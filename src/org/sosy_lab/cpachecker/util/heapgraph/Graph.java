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

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

/*
 * Formal definition of graph is here: https://cloud.githubusercontent.com/assets/1872537/11579439/ec22840e-99e2-11e5-8cdb-36f17e3f9d09.png
 * Description wrt current implementation:
 * - Set of nodes (maintained by class Graph)
 * - V : N x Vars_H -> B3
 * - P : N x Vars_P -> B3
 * - E : N x Fields x N -> B3 (edge status stored by edge)
 */
public class Graph {
  // Defining values in three-valued logic
  public enum ThreeVal {
    FALSE,
    TRUE,
    MAYBE
  }

  public BooleanFormula nodeFormula;

  public Set<Node> nodes;
  public Set<Edge> edges; // E: N x Fields x N -> B3. For optimality, only store TRUE/MAYBE edges
  public HeapVarLabeling heapVarLabels; // V: N x Vars_H -> B3

  private boolean isUniversal; // heap is universal - symbolic
  private boolean isEmpty; // heap is empty - symbolic

  public Graph() {
    this.nodes = new TreeSet<>();
    this.edges = new HashSet<>();
    this.isUniversal = false;
    this.isEmpty = false;
  }

  public static Graph universalHeap() {
    Graph univ = new Graph();
    univ.isUniversal = true;
    univ.isEmpty = false;
    return univ;
  }

  public static Graph emptyHeap() {
    Graph empt = new Graph();
    empt.isEmpty = true;
    empt.isUniversal = false;
    return empt;
  }

  // graph editing functions
  public void addNode(Node n) {
    // TODO write this
  }
}