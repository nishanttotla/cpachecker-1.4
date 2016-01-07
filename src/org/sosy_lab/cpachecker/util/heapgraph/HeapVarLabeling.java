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
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.util.heapgraph.Graph.ThreeVal;

/*
 * This class stores heap variable labelings, as they exist in a current graph.
 * More heap variables get added as they're encountered, and a labeling from variable
 * to nodes is stored here.
 */

public class HeapVarLabeling {
  private class HVEdge {
    public Node node;
    public String heapVar;

    public HVEdge(Node node, String var) {
      this.node = node;
      this.heapVar = var;
    }
  }

  public Set<String> heapVarsSeen; // all heapvars seen so far, need to be tracked in the graph
  public HashMap<HVEdge, ThreeVal> heapVarLabels; // V: N x Vars_H -> B3. For optimality, only store TRUE/MAYBE edges

  public HeapVarLabeling() {
    this.heapVarsSeen = new HashSet<>();
    this.heapVarLabels = new HashMap<>();
  }

  public void addNewHeapVar(String var) {
    heapVarsSeen.add(var);
    // TODO add maybe edges to all nodes
  }

  // adds a new node+heapvar pair with value TRUE
  public void addNewNodeWithHeapVarAssignment(Node node, String var) {
    HVEdge newEdge = new HVEdge(node, var);
    // TODO if any MAYBE edges exist for var, delete them from the map
    heapVarLabels.put(newEdge, ThreeVal.TRUE);
  }

  public ThreeVal getHeapVarAssignmentStatus(Node node, String var) {
    HVEdge queryEdge = new HVEdge(node, var);
    if(heapVarLabels.containsKey(queryEdge)) {
      return heapVarLabels.get(queryEdge);
    }
    return ThreeVal.FALSE;
  }

  // used for var1 := var2
  public void copyHeapVar(String var1, String var2) {
    // TODO delete all occurences for var1
    // TODO iterate through all occurences for var2, and copy those for var 1
  }

  public Set<Node> getAllNodesPointedByHeapVar(String var, Set<Node> allNodes) {
    // TODO iterate over all nodes in allNodes and find those that var points to (True or Maybe)
    Set<Node> pointedNodes = new HashSet<>();
    return pointedNodes;
  }
}