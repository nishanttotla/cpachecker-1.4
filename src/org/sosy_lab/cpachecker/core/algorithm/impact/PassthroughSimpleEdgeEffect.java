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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;

public class PassthroughSimpleEdgeEffect extends SimpleEdgeEffect {
  public PassthroughSimpleEdgeEffect(CFAEdge pEdge) {
    super(pEdge);
    CFAEdgeType edgeType = pEdge.getEdgeType();
    if(edgeType == CFAEdgeType.DeclarationEdge) {
      CDeclaration dcl = ((CDeclarationEdge) pEdge).getDeclaration();
      System.out.println("Decl type is "+ dcl.getType());
      if(dcl.getType() instanceof CPointerType) {
        CPointerType ptrType = (CPointerType)dcl.getType();
        if(ptrType.getType() instanceof CComplexType) {
          CComplexType objType = (CComplexType)ptrType.getType();
          if(objType.getKind() == CComplexType.ComplexTypeKind.STRUCT) {
            // we assume only one struct exists in the program
            // when a pointer to that struct is found in a declaration, we need to keep track of it
            System.out.println("FOUND POINTER TO STRUCT");
          }
        }
      }
    }
  }

  @Override
  public Footprint apply(BooleanFormulaManagerView pBfmgr, Vertex pPrev, Footprint pF) {
    return pF;
  }
}