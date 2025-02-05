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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;

public class CopySimpleEdgeEffect extends SimpleEdgeEffect {
  String varLhs;
  String varRhs;

  public CopySimpleEdgeEffect(CFAEdge pEdge, CExpression lhs, CExpression rhs) {
    super(pEdge);
    opType = OpType.COPY;
    // TODO call this from EdgeEffect
  }

  @Override
  public Footprint apply(BooleanFormulaManagerView pBfmgr, Vertex pPrev, Footprint pF) {
    return new Footprint(pF, pBfmgr.makeVariable(varLhs));
  }
}