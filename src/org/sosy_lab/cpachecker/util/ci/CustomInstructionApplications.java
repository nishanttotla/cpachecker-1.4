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
package org.sosy_lab.cpachecker.util.ci;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.ImmutableMap;


public class CustomInstructionApplications {

  private final ImmutableMap<CFANode, AppliedCustomInstruction> cis;

  /**
   * Constructor of CustomInstructionApplications
   * @param pCis ImmutableMap
   */
  public CustomInstructionApplications(final ImmutableMap<CFANode, AppliedCustomInstruction> pCis) {
    cis = pCis;
  }

  /**
   * Checks if the ImmutableMap cis contains the given CFANode
   * (after it is extracted out of the AbstractState)
   * @param pState AbstractState
   * @return true if cis contains the given node
   * @throws CPAException if the given node can't be extracted
   */
  public boolean isStartState(final AbstractState pState) throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);
    if (locState == null) {
      throw new CPAException("TheState " + pState+ " has to contain a location state!");
    }
    return cis.containsKey(locState);
  }

  /**
   * Checks if the given AbstractState pIsEnd is an endNode of the given AbsractState pCISart
   * @param pIsEnd AbstractState
   * @param pCIStart AbstractState
   * @return true if pIsEnd is an endNode of pCISart
   * @throws CPAException
   */
  public boolean isEndState(final AbstractState pIsEnd, final AbstractState pCIStart) throws CPAException {
    return isEndState(pIsEnd, AbstractStates.extractLocation(pCIStart));
  }

  /**
   * Checks if the given AbstractState pIsEnd is an endNode of the given CFANode pCISart
   * @param pIsEnd AbstractState
   * @param pCIStart CFANode
   * @return true if pIsEnd is an endNode of pCISart
   * @throws CPAException
   */
  public boolean isEndState(final AbstractState pIsEnd, final CFANode pCIStart) throws CPAException {
    return cis.get(pCIStart).isEndState(pIsEnd);
  }

}
