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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


class BaseVisitor implements CExpressionVisitor<Variable, UnrecognizedCCodeException>{

  public BaseVisitor(final CFAEdge cfaEdge, final PointerTargetSetBuilder pts) {
    this.cfaEdge = cfaEdge;
    this.pts = pts;
  }

  @Override
  public Variable visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Variable visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Variable visit(final CCastExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Variable visit(final CComplexCastExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public Variable visit(CFieldReference e) throws UnrecognizedCCodeException {

    e = CToFormulaConverterWithPointerAliasing.eliminateArrow(e, cfaEdge);

    final Variable base = e.getFieldOwner().accept(this);
    if (base != null) {
      return Variable.create(base.getName()  + CToFormulaConverterWithPointerAliasing.FIELD_NAME_SEPARATOR + e.getFieldName(),
                             CTypeUtils.simplifyType(e.getExpressionType()));
    } else {
      return null;
    }
  }

  @Override
  public Variable visit(final CIdExpression e) throws UnrecognizedCCodeException {
    CType type = CTypeUtils.simplifyType(e.getExpressionType());
    if (!pts.isActualBase(e.getDeclaration().getQualifiedName()) &&
        !CTypeUtils.containsArray(type)) {
      lastBase = Variable.create(e.getDeclaration().getQualifiedName(), type);
      return lastBase;
    } else {
      return null;
    }
  }

  @Override
  public Variable visit(final CCharLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Char literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(final CFloatLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Float literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(final CIntegerLiteralExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Variable visit(final CStringLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("String literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(CImaginaryLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Imaginary literal in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(final CTypeIdExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("TypeId in place of lvalue", cfaEdge, e);
  }

  @Override
  public Variable visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    switch (e.getOperator()) {
    case AMPER:
      throw new UnrecognizedCCodeException("Address in place of lvalue", cfaEdge, e);
    case TILDE:
    case MINUS:
      throw new UnrecognizedCCodeException("Arithmetic in place of lvalue", cfaEdge, e);
    case SIZEOF:
      throw new UnrecognizedCCodeException("Constant in place of lvalue", cfaEdge, e);
    default:
      throw new UnrecognizedCCodeException("Unrecognized code in place of lvalue", cfaEdge, e);
    }
  }

  @Override
  public Variable visit(final CPointerExpression e) throws UnrecognizedCCodeException {
    return null;
  }

  public Variable getLastBase() {
    return lastBase;
  }

  private final PointerTargetSetBuilder pts;
  private final CFAEdge cfaEdge;

  private Variable lastBase = null;
}
