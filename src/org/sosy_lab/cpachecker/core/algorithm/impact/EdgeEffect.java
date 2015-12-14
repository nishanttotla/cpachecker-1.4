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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.util.heapgraph.Graph;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;

/*
  Abstract class, starting to define edge effects (posts)
 */
public abstract class EdgeEffect {
  enum OpType {
    LOAD,
    STORE,
    COPY,
    DATA,
    ALLOC
  }

  OpType opType;

  public EdgeEffect() {

  }

  // instantiate appropriate edge effect depending on operation
  public static EdgeEffect create(CFAEdge pEdge) {
    CFAEdgeType edgeType = pEdge.getEdgeType();

    // make cases for each type of edge and create appropriate EdgeEffect object
    return null;
  }

  private static EdgeEffect createStatementEffect(CStatementEdge pEdge) {
    CStatement stmt = pEdge.getStatement();

    // TODO case for COPY still missing
    if(stmt instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement funcAssgn = (CFunctionCallAssignmentStatement)stmt;
      String funcName = StmtUtil.getFunction(funcAssgn.getRightHandSide());
      if(funcName == "malloc") {
        return new AllocSimpleEdgeEffect(pEdge);
      }
    } else if(stmt instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement assgn = (CExpressionAssignmentStatement)stmt;
      if(StmtUtil.hasDereference(assgn.getRightHandSide()) != null) {
        // load (rhs dereference)
        return new LoadSimpleEdgeEffect(pEdge, assgn.getLeftHandSide(), assgn.getRightHandSide());
      } else if(StmtUtil.hasDereference(assgn.getLeftHandSide()) != null) {
        // store (lhs dereference)
        return new StoreSimpleEdgeEffect(pEdge, assgn.getLeftHandSide(), assgn.getRightHandSide());
      } else {
        // data op
        return new DataOpSimpleEdgeEffect(pEdge);
      }
    } else {
      System.out.println("Unknown statement of class: " + stmt.getClass());
      assert(false);
    }
    return null;
  }

  public Graph apply(Vertex v, Graph pre) {
    return null;
  }

  public abstract Footprint apply(BooleanFormulaManagerView pBfmgr, Vertex pPrev, Footprint pF);
}