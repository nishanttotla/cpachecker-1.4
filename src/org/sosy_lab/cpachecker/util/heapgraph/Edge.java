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

// Define edge for a heap graph
public class Edge {
  public ThreeVal status;
  public Node source;
  public Node destination;
  public String field; // name of the outgoing field from source
  // currently rest of the code assumes a single field, and will not work if edges
  // with multiple field types are present

  public Edge(Node src, Node dst, String field, ThreeVal st) {
    this.status = st;
    this.source = src;
    this.destination = dst;
    this.field = field;
  }
}