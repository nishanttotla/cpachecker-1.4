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

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.heapgraph.Graph;
import org.sosy_lab.cpachecker.util.heapgraph.Node;


/*
  This class defines functions for transforming heaps across edges of specific types
 */

class HeapTransfer {
  public HeapTransfer() {

  }

  public Graph post(Vertex v, CFAEdge edge, Graph pre) {
    return null;
  }

  /*
   * ALLOC (v := alloc())
   * Create new node n that is not in N.
   * N' = N U {n}
   * V' updates V so that V'(v,n) = True, and for all m≠n, V'(v,m) = False
   * E' updates E so that E'(n,f,m) = False for all fields f.
   */
  public Graph applyAlloc(AllocSimpleEdgeEffect allocEffect, Vertex v, Graph pre) {
    String heapVar = allocEffect.getHeapVar();
    Node allocNode = new Node(pre.nodeFormula, true); // new node is a root
    Graph post = pre;
    post.addNode(allocNode);
    // TODO add functions for updating heapvar assignments for new node
    post.heapVarLabels.addNewNodeWithHeapVarAssignment(allocNode, heapVar);
    return post;
  }

  /*
   * STORE (v1->n := v2)
   * N' = N
   * V' = V
   * E' updates E as follows:
   * Let S = {n ∈ N : V(v1,n) = True or V(v1,n) = Maybe}
   * Let T = {n ∈ N : V(v2,n) = True or V(v2,n) = Maybe}
   * Then E'(s,f,t) = Maybe, where s ∈ S, t ∈ T (and True if both S and T are singletons)
   */
  public Graph applyStore(StoreSimpleEdgeEffect storeEffect, Vertex v, Graph pre) {
    Dereference storeDeref = storeEffect.deref;
    Graph post = pre;
    if(storeDeref.isPointerField) {
      Set<Node> nodesSrcVar = post.heapVarLabels.getAllNodesPointedByHeapVar(storeEffect.srcVar);
      Set<Node> nodesDerefVar = post.heapVarLabels.getAllNodesPointedByHeapVar(storeDeref.varName);
      // TODO take intersection and finish by updating edges
    } else {
      // TODO perhaps update predicate status inside the node
    }
    return post;
  }

  /*
   * LOAD (v1 := v2->n)
   * N' = N
   * V' updates V as follows:
   * Let S = {n ∈ N : V(v2,n) = True or V(v2,n) = Maybe}
   * Let T = {n ∈ N : E(s,f,n) = True or E(s,f,n) = Maybe where s ∈ S}
   * if |T| = {t} (singleton), then V'(v1,t) = True, otherwise V'(v1,t) = Maybe for all t ∈ T
   * E' = E
   */
  public Graph applyLoad(LoadSimpleEdgeEffect loadEffect, Vertex v, Graph pre) {
    Dereference loadDeref = loadEffect.deref;
    Graph post = pre;
    if(loadDeref.isPointerField) {
      Set<Node> nodesDestVar = post.heapVarLabels.getAllNodesPointedByHeapVar(loadEffect.varLhs);
    } else {
      // TODO probably nothing to do here, since data field is being updated
    }
    return post;
  }

  /*
   * COPY (v1 := v2)
   * N' = N
   * V' updates V so that for each node n ∈ N, V'(v1, n) = V(v2, n)
   * E' = E
   */
  public Graph applyCopy(CopySimpleEdgeEffect copyEffect, Vertex v, Graph pre) {
    Graph post = pre;
    return post;
  }

  public Graph applyDataOp(DataOpSimpleEdgeEffect dataOpEffect, Vertex v, Graph pre) {
    Graph post = pre;
    return post;
  }

  public Graph applyPassthrough(PassthroughSimpleEdgeEffect passthroughEffect, Vertex v, Graph pre) {
    Graph post = pre;
    return post;
  }

  public Graph applyDeclaration(CSimpleDeclaration decl, Graph pre) {
    return null;
  }

  public static CSimpleDeclaration extractDeclarationUsed(CExpression expr) {
    return null;
  }
}