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

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;

/*
  A bunch of utilities for manipulating heap graphs
 */
public class StmtUtil {
  // get function name from call
  public static String getFunction(CFunctionCallExpression pRightHandSide) {
    CFunctionDeclaration decl = pRightHandSide.getDeclaration();

    CExpression nameExpr = pRightHandSide.getFunctionNameExpression();
    return nameExpr.toString();
  }

  // get variable name from expression
  public static String getVariableName(CExpression expr) {
    return expr.toString();
  }

  // check if the expression contains a pointer dereference, and return it
  public static Dereference hasDereference(CExpression expr) {
    if(expr instanceof CIntegerLiteralExpression) {
      return null;
    } else if(expr instanceof CFieldReference) {
      CFieldReference fieldExpr = (CFieldReference)expr;
      String fieldName = fieldExpr.getFieldName();
      String varName = getVariableName(fieldExpr.getFieldOwner());
      return new Dereference(fieldName, varName);
    } else if(expr instanceof CIdExpression) {
      // reference to a variable, not a dereference
      return null;
    } else if(expr instanceof CBinaryExpression) {
      return null;
    } else {
      System.out.println("[StmtUtil.hasDereference] unknown expr type " + expr.getClass() + " " + expr.getExpressionType());
      assert(false);
    }
    return null;
  }
}