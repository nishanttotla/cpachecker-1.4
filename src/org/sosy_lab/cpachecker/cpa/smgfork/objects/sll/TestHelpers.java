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
package org.sosy_lab.cpachecker.cpa.smgfork.objects.sll;

import org.sosy_lab.cpachecker.cpa.smgfork.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smgfork.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGRegion;

public final class TestHelpers {
  static public final Integer createList(CLangSMG pSmg, int pLength, int pSize, int pOffset, String pPrefix) {
    Integer value = null;
    for (int i = 0; i < pLength; i++) {
      SMGObject node = new SMGRegion(pSize, pPrefix + "list_node" + i);
      SMGEdgeHasValue hv;
      if (value == null) {
        hv = new SMGEdgeHasValue(pSize, 0, node, 0);
      } else {
        hv = new SMGEdgeHasValue(AnonymousTypes.dummyPointer, pOffset, node, value);
      }
      value = SMGValueFactory.getNewValue();
      SMGEdgePointsTo pt = new SMGEdgePointsTo(value, node, 0);
      pSmg.addHeapObject(node);
      pSmg.addValue(value);
      pSmg.addHasValueEdge(hv);
      pSmg.addPointsToEdge(pt);
    }
    return value;
  }

  static public final SMGEdgeHasValue createGlobalList(CLangSMG pSmg, int pLength, int pSize, int pOffset, String pVariable) {
    Integer value = TestHelpers.createList(pSmg, pLength, pSize, pOffset, pVariable);
    SMGRegion globalVar = new SMGRegion(8, pVariable);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(AnonymousTypes.dummyPointer, 0, globalVar, value);
    pSmg.addGlobalObject(globalVar);
    pSmg.addHasValueEdge(hv);

    return hv;
  }

  private TestHelpers(){}
}
