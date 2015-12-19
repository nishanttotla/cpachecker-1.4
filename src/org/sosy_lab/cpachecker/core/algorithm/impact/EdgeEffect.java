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

import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
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
    // TODO need to handle more cases
    if(edgeType == CFAEdgeType.AssumeEdge) {
      return new PassthroughSimpleEdgeEffect(pEdge);
    } else if(edgeType == CFAEdgeType.BlankEdge) {
      return new PassthroughSimpleEdgeEffect(pEdge);
    } else if(edgeType == CFAEdgeType.DeclarationEdge) {
      CDeclaration dcl = ((CDeclarationEdge) pEdge).getDeclaration();
      return new PassthroughSimpleEdgeEffect(pEdge);
    } else if(edgeType == CFAEdgeType.ReturnStatementEdge) {
      return new PassthroughSimpleEdgeEffect(pEdge);
    } else if(edgeType == CFAEdgeType.StatementEdge) {
      return createStatementEffect((CStatementEdge)pEdge);
    } else {
      System.out.println("Unknown edge type in EdgeEffect");
      assert(false);
    }
    return null;
  }

  // for a statement that does heap manipulation, create the appropriate effect
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
      if(StmtUtil.getDereference(assgn.getRightHandSide()) != null) {
        // load (rhs dereference)
        return new LoadSimpleEdgeEffect(pEdge, assgn.getLeftHandSide(), assgn.getRightHandSide());
      } else if(StmtUtil.getDereference(assgn.getLeftHandSide()) != null) {
        // store (lhs dereference)
        return new StoreSimpleEdgeEffect(pEdge, assgn.getLeftHandSide(), assgn.getRightHandSide());
      } else {
        // data op
        return new DataOpSimpleEdgeEffect(pEdge);
      }
    } else if(stmt instanceof CExpressionStatement) {
      // TODO this is a stop-gap case, delete this if else has no need for assert(false)
      return null;
    } else {
      System.out.println("Unknown statement of class: " + stmt.getClass());
      assert(false);
    }
    return null;
  }

  public Graph apply(Vertex v, Graph pre) {
    // TODO need to write this out
    return pre;
  }

  // function to keep track of all terms encountered during the algorithm
  public abstract Footprint apply(BooleanFormulaManagerView pBfmgr, Vertex pPrev, Footprint pF);
}