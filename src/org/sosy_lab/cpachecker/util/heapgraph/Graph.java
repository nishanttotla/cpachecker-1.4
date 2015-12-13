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
import java.util.TreeSet;

/*
  Formal definition of graph is here: https://cloud.githubusercontent.com/assets/1872537/11579439/ec22840e-99e2-11e5-8cdb-36f17e3f9d09.png
 */
public class Graph {
  // Defining values in three-valued logic
  public enum ThreeVal {
    FALSE,
    TRUE,
    MAYBE
  }

  private class HVEdge {
    public Node node;
    public HeapVar var;

    public HVEdge(Node node, HeapVar var) {
      this.node = node;
      this.var = var;
    }
  }

  public Set<Node> nodes;
  public HashMap<Edge, ThreeVal> edges; // E: N x Fields x N -> B3
  public HashMap<HVEdge, ThreeVal> heapVarLabeling; // V: N x Vars_h -> B3

  public Graph() {
    this.nodes = new TreeSet<>();
    this.edges = new HashMap<>();
    this.heapVarLabeling = new HashMap<>();
  }
}