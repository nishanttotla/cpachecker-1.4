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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;


public class NumeralFormulaManagerView
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends BaseManagerView
        implements NumeralFormulaManager<ParamFormulaType, ResultFormulaType> {

  private final NumeralFormulaManager<ParamFormulaType, ResultFormulaType> manager;

  public NumeralFormulaManagerView(FormulaManagerView pViewManager,
      NumeralFormulaManager<ParamFormulaType, ResultFormulaType> pManager) {
    super(pViewManager);
    this.manager = pManager;
  }

  private BooleanFormula wrapInView(BooleanFormula pFormula) {
    return pFormula;
  }

  private BooleanFormula extractFromView(BooleanFormula pCast) {
    return pCast;
  }

  @Override
  public ResultFormulaType negate(ParamFormulaType pNumber) {
    return manager.negate(pNumber);
  }

  @Override
  public ResultFormulaType add(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return manager.add(pNumber1, pNumbe2);
  }

  @Override
  public ResultFormulaType sum(List<ParamFormulaType> operands) {
    return manager.sum(operands);
  }

  @Override
  public ResultFormulaType subtract(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return manager.subtract(pNumber1, pNumbe2);
  }
  @Override
  public ResultFormulaType divide(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return manager.divide(pNumber1, pNumbe2);
  }
  @Override
  public ResultFormulaType modulo(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return manager.modulo(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula modularCongruence(ParamFormulaType pNumber1, ParamFormulaType pNumber2, long pModulo) {
    return wrapInView(manager.modularCongruence(pNumber1, pNumber2, pModulo));
  }
  @Override
  public ResultFormulaType multiply(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return manager.multiply(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula equal(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return wrapInView(manager.equal(pNumber1, pNumbe2));
  }
  @Override
  public BooleanFormula greaterThan(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return wrapInView(manager.greaterThan(pNumber1, pNumbe2));
  }
  @Override
  public BooleanFormula greaterOrEquals(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return wrapInView(manager.greaterOrEquals(pNumber1, pNumbe2));
  }
  @Override
  public BooleanFormula lessThan(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return wrapInView(manager.lessThan(pNumber1, pNumbe2));
  }
  @Override
  public BooleanFormula lessOrEquals(ParamFormulaType pNumber1, ParamFormulaType pNumbe2) {
    return wrapInView(manager.lessOrEquals(pNumber1, pNumbe2));
  }


  @Override
  public boolean isEqual(BooleanFormula pNumber) {
    return manager.isEqual(extractFromView(pNumber));
  }


  @Override
  public ResultFormulaType makeNumber(long pI) {
    return manager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(BigInteger pI) {
    return manager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(String pI) {
    return manager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(double pNumber) {
    return manager.makeNumber(pNumber);
  }

  @Override
  public ResultFormulaType makeNumber(BigDecimal pNumber) {
    return manager.makeNumber(pNumber);
  }

  @Override
  public ResultFormulaType makeVariable(String pVar) {
    return manager.makeVariable(pVar);
  }

  public ResultFormulaType makeVariable(String pVar, int idx) {
    return manager.makeVariable(FormulaManagerView.makeName(pVar, idx));
  }

  @Override
  public FormulaType<ResultFormulaType> getFormulaType() {
    return manager.getFormulaType();
  }

}
