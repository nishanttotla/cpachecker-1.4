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
package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.type.ArrayValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.EnumConstantValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValueFormula;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValueFormula.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicValueFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@Options(prefix="cpa.value")
public class ValueAnalysisTransferRelation extends ForwardingTransferRelation<ValueAnalysisState, ValueAnalysisState, VariableTrackingPrecision> {
  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  private boolean symbolicValues = new SymbolicValuesOption().areSymbolicValuesEnabled();

  @Option(secure=true, description = "if there is an assumption like (x!=0), "
      + "this option sets unknown (uninitialized) variables to 1L, "
      + "when the true-branch is handled.")
  private boolean initAssumptionVars = false;

  @Option(secure=true, description = "Process the Automaton ASSUMEs as if they were statements, not as if they were"
      + " assumptions.")
  private boolean automatonAssumesAsStatements = false;

  @Option(secure=true, description = "Assume that variables used only in a boolean context are either zero or one.")
  private boolean optimizeBooleanVariables = true;

  @Option(secure=true, description = "Track Java array values in explicit value analysis. " +
      "This may be costly if the verified program uses big or lots of arrays. " +
      "Arrays in C programs will always be tracked, even if this value is false.")
  private boolean trackJavaArrayValues = true;

  private final Set<String> javaNonStaticVariables = new HashSet<>();

  private JRightHandSide missingInformationRightJExpression = null;
  private String missingInformationLeftJVariable = null;

  private boolean missingFieldVariableObject;
  private Pair<String, Value> fieldNameAndInitialValue;

  private boolean missingScopedFieldName;
  private JIdExpression notScopedField;
  private Value notScopedFieldValue;

  private boolean missingAssumeInformation;

  /**
   * This List is used to communicate the missing
   * Information needed from other cpas.
   * (at the moment specifically SMG)
   */
  private List<MissingInformation> missingInformationList;

  /**
   * Save the old State for strengthen.
   */
  private ValueAnalysisState oldState;

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  private final Collection<String> addressedVariables;
  private final Collection<String> booleanVariables;

  public ValueAnalysisTransferRelation(Configuration config, LogManager pLogger, CFA pCfa) throws InvalidConfigurationException {
    config.inject(this);
    machineModel = pCfa.getMachineModel();
    logger = new LogManagerWithoutDuplicates(pLogger);

    if (pCfa.getVarClassification().isPresent()) {
      addressedVariables = pCfa.getVarClassification().get().getAddressedVariables();
      booleanVariables   = pCfa.getVarClassification().get().getIntBoolVars();
    } else {
      addressedVariables = ImmutableSet.of();
      booleanVariables   = ImmutableSet.of();
    }
  }

  @Override
  protected Collection<ValueAnalysisState> postProcessing(ValueAnalysisState successor) {
    // always return a new state (requirement for strengthening states with interpolants)
    if (successor != null) {
      successor = ValueAnalysisState.copyOf(successor);
    }

    return super.postProcessing(successor);
  }


  @Override
  protected void setInfo(AbstractState pAbstractState,
      Precision pAbstractPrecision, CFAEdge pCfaEdge) {
    super.setInfo(pAbstractState, pAbstractPrecision, pCfaEdge);
    // More than 5 function parameters is sufficiently seldom.
    // For any other cfaEdge we need only a list of length 1.
    // In principle it is unnecessary to always create a new list
    // but I'm not sure of the behavior of calling strengthen, so
    // it is more secure.
    missingInformationList = new ArrayList<>(5);
    oldState = ValueAnalysisState.copyOf((ValueAnalysisState) pAbstractState);
  }

  @Override
  protected ValueAnalysisState handleMultiEdge(final MultiEdge cfaEdge) throws CPATransferException {
    // we need to keep the old state,
    // because the analysis uses a 'delta' for the now state
    final ValueAnalysisState backup = state;
    for (CFAEdge edge : cfaEdge) {
      state = handleSimpleEdge(edge);
    }
    final ValueAnalysisState successor = state;
    state = backup;
    return successor;
  }

  @Override
  protected ValueAnalysisState handleFunctionCallEdge(FunctionCallEdge callEdge,
      List<? extends AExpression> arguments, List<? extends AParameterDeclaration> parameters,
      String calledFunctionName) throws UnrecognizedCCodeException {
    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);

    assert (parameters.size() == arguments.size())
        || callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs();

    // visitor for getting the values of the actual parameters in caller function context
    final ExpressionValueVisitor visitor = getVisitor();

    // get value of actual parameter in caller function context
    for (int i = 0; i < parameters.size(); i++) {
      Value value;
      AExpression exp = arguments.get(i);

      if (exp instanceof JExpression) {
        value = ((JExpression) exp).accept(visitor);
      } else if (exp instanceof CExpression) {
        value = visitor.evaluate((CExpression) exp, (CType) parameters.get(i).getType());
      } else {
        throw new AssertionError("Unknown expression: " + exp);
      }

      String paramName = parameters.get(i).getName();

      MemoryLocation formalParamName = MemoryLocation.valueOf(calledFunctionName, paramName, 0);

      if (value.isUnknown()) {
        newElement.forget(formalParamName);

        if (isMissingCExpressionInformation(visitor, exp)) {
          addMissingInformation(formalParamName, exp);
        }
      } else {
        newElement.assignConstant(formalParamName, value, parameters.get(i).getType());
      }

      visitor.reset();

    }

    return newElement;
  }

  @Override
  protected ValueAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      assert "default return".equals(cfaEdge.getDescription())
              || "skipped uneccesary edges".equals(cfaEdge.getDescription());

      // clone state, because will be changed through removing all variables of current function's scope
      state = ValueAnalysisState.copyOf(state);
      state.dropFrame(functionName);
    }

    return state;
  }

  @Override
  protected ValueAnalysisState handleReturnStatementEdge(AReturnStatementEdge returnEdge)
          throws UnrecognizedCCodeException {

    // visitor must use the initial (previous) state, because there we have all information about variables
    ExpressionValueVisitor evv = new ExpressionValueVisitor(state, functionName, machineModel, logger, symbolicValues);

    // clone state, because will be changed through removing all variables of current function's scope
    state = ValueAnalysisState.copyOf(state);
    state.dropFrame(functionName);

    AExpression expression = returnEdge.getExpression().orNull();
    if (expression == null && returnEdge instanceof CReturnStatementEdge) {
      expression = CIntegerLiteralExpression.ZERO; // this is the default in C
    }

    FunctionEntryNode functionEntryNode = returnEdge.getSuccessor().getEntryNode();

    MemoryLocation functionReturnVar = null;
    if(functionEntryNode.getReturnVariable().isPresent()) {
      functionReturnVar = MemoryLocation.valueOf(functionEntryNode.getReturnVariable().get().getQualifiedName());
    }

    if (expression != null && functionReturnVar != null) {

      return handleAssignmentToVariable(functionReturnVar,
          functionEntryNode.getFunctionDefinition().getType().getReturnType(), // TODO easier way to get type?
          expression,
          evv);
    } else {
      return state;
    }
  }

  /**
   * Handles return from one function to another function.
   * @param functionReturnEdge return edge from a function to its call site
   * @return new abstract state
   */
  @Override
  protected ValueAnalysisState handleFunctionReturnEdge(FunctionReturnEdge functionReturnEdge,
      FunctionSummaryEdge summaryEdge, AFunctionCall exprOnSummary, String callerFunctionName)
    throws UnrecognizedCodeException {

    ValueAnalysisState newElement  = ValueAnalysisState.copyOf(state);

    Optional<? extends AVariableDeclaration> returnVarName = functionReturnEdge.getFunctionEntry().getReturnVariable();
    MemoryLocation functionReturnVar = null;
    if(returnVarName.isPresent()) {
      functionReturnVar = MemoryLocation.valueOf(returnVarName.get().getQualifiedName());
    }

    // expression is an assignment operation, e.g. a = g(b);

    if (exprOnSummary instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)exprOnSummary);
      AExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable

      if (op1 instanceof CLeftHandSide) {
        ExpressionValueVisitor v =
            new ExpressionValueVisitor(state, callerFunctionName,
                machineModel, logger, symbolicValues);
        MemoryLocation assignedVarName = v.evaluateMemoryLocation((CLeftHandSide) op1);

        boolean valueExists = state.contains(functionReturnVar);

        if (assignedVarName == null) {
          if (v.hasMissingPointer() && valueExists) {
            Value value = state.getValueFor(functionReturnVar);
            addMissingInformation((CLeftHandSide) op1, value);
          }
        } else if (valueExists) {
          Value value = state.getValueFor(functionReturnVar);
          newElement.assignConstant(assignedVarName, value, state.getTypeForMemoryLocation(functionReturnVar));
        } else {
          newElement.forget(assignedVarName);
        }

      } else if (op1 instanceof AIdExpression) {
        String assignedVarName = ((AIdExpression) op1).getDeclaration().getQualifiedName();

        if (!state.contains(functionReturnVar)) {
          newElement.forget(assignedVarName);
        } else if (op1 instanceof JIdExpression && isDynamicField((JIdExpression)op1)) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
          notScopedFieldValue = state.getValueFor(functionReturnVar);
        } else {
          newElement.assignConstant(assignedVarName, state.getValueFor(functionReturnVar));
        }
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current element
      else if (op1 instanceof APointerExpression) {
      } else {
        throw new UnrecognizedCodeException("on function return", summaryEdge, op1);
      }
    }

    if(returnVarName.isPresent()) {
      newElement.forget(functionReturnVar);
    }

    return newElement;
  }

  private boolean isDynamicField(JIdExpression pIdentifier) {
    final JSimpleDeclaration declaration = pIdentifier.getDeclaration();

    return (declaration instanceof JFieldDeclaration)
        && !((JFieldDeclaration) declaration).isStatic();
  }

  @Override
  protected ValueAnalysisState handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    ValueAnalysisState newState = ValueAnalysisState.copyOf(state);
    AFunctionCall functionCall  = cfaEdge.getExpression();

    if (functionCall instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignment = ((AFunctionCallAssignmentStatement)functionCall);
      AExpression leftHandSide = assignment.getLeftHandSide();

      if (leftHandSide instanceof CLeftHandSide) {
        MemoryLocation assignedMemoryLocation = getVisitor().evaluateMemoryLocation((CLeftHandSide) leftHandSide);

        if (newState.contains(assignedMemoryLocation)) {
          newState.forget(assignedMemoryLocation);
        }
      }
    }

    return newState;
  }

  @Override
  protected ValueAnalysisState handleAssumption(AssumeEdge cfaEdge, AExpression expression, boolean truthValue)
    throws UnrecognizedCCodeException {

    final ExpressionValueVisitor evv = getVisitor();
    final Type booleanType = getBooleanType(expression);

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    Value value = getExpressionValue(expression, booleanType, evv);

    if (!value.isExplicitlyKnown()) {
      ValueAnalysisState element = ValueAnalysisState.copyOf(state);

      // If it's a symbolic formula, try if we can solve it for any of its symbolic values.
      if (value instanceof SymbolicValueFormula) {
        Pair<SymbolicValue, Value> replacement = null;
        replacement = ((SymbolicValueFormula)value).inferAssignment(truthValue, logger);
        if (replacement != null) {
          for (MemoryLocation memloc : state.getTrackedMemoryLocations()) {
            Value trackedValue = state.getValueFor(memloc);
            if (trackedValue instanceof SymbolicValueFormula) {
              SymbolicValueFormula trackedFormula = (SymbolicValueFormula) trackedValue;
              Value newValue = trackedFormula.replaceSymbolWith(replacement.getFirst(), replacement.getSecond(), logger);
              if (newValue != trackedValue) {
                element.assignConstant(memloc, newValue, state.getTypeForMemoryLocation(memloc));
              }
            }
          }
        }
      }

      AssigningValueVisitor avv = new AssigningValueVisitor(element, truthValue, booleanVariables);

      if (expression instanceof JExpression && ! (expression instanceof CExpression)) {

        ((JExpression) expression).accept(avv);

        if (avv.hasMissingFieldAccessInformation()) {
          assert missingInformationRightJExpression != null;
          missingAssumeInformation = true;
        }

      } else {
        ((CExpression) expression).accept(avv);
      }

      if (isMissingCExpressionInformation(evv, expression)) {
        missingInformationList.add(new MissingInformation(truthValue, expression));
      }

      return element;

    } else if (representsBoolean(value, truthValue)) {
      // we do not know more than before, and the assumption is fulfilled, so return a copy of the old state
      // we need to return a copy, otherwise precision adjustment might reset too much information, even on the original state
      return ValueAnalysisState.copyOf(state);

    } else {
      // assumption not fulfilled
      return null;
    }
  }

  private Type getBooleanType(AExpression pExpression) {
    if (pExpression instanceof JExpression) {
      return JSimpleType.getBoolean();
    } else if (pExpression instanceof CExpression) {
      return CNumericTypes.INT;

    } else {
      throw new AssertionError("Unhandled expression type " + pExpression.getClass());
    }
  }

  /*
   *  returns 'true' if the given value represents the specified boolean bool.
   *  A return of 'false' does not necessarily mean that the given value represents !bool,
   *  but only that it does not represent bool.
   *
   *  For example:
   *    * representsTrue(BooleanValue.valueOf(true), true)  = true
   *    * representsTrue(BooleanValue.valueOf(false), true) = false
   *  but:
   *    * representsTrue(NullValue.getInstance(), true)     = false
   *    * representsTrue(NullValue.getInstance(), false)    = false
   *
   */
  private boolean representsBoolean(Value value, boolean bool) {
    if (value instanceof BooleanValue) {
      return ((BooleanValue) value).isTrue() == bool;

    } else if (value.isNumericValue()) {
      return ((NumericValue) value).equals(new NumericValue(bool ? 1L : 0L));

    } else {
      return false;
    }
  }


  @Override
  protected ValueAnalysisState handleDeclarationEdge(ADeclarationEdge declarationEdge, ADeclaration declaration)
    throws UnrecognizedCCodeException {

    if (!(declaration instanceof AVariableDeclaration) || !isTrackedType(declaration.getType())) {
      // nothing interesting to see here, please move along
      return state;
    }

    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);
    AVariableDeclaration decl = (AVariableDeclaration) declaration;
    Type declarationType = decl.getType();

    // get the variable name in the declarator
    String varName = decl.getName();

    Value initialValue = getDefaultInitialValue(decl);

    // get initializing statement
    AInitializer init = decl.getInitializer();

    // handle global variables
    if (decl.isGlobal()) {
      if (decl instanceof JFieldDeclaration && !((JFieldDeclaration) decl).isStatic()) {
        missingFieldVariableObject = true;
        javaNonStaticVariables.add(varName);
      }
    }

    MemoryLocation memoryLocation;

    // assign initial value if necessary
    if (decl.isGlobal()) {
      memoryLocation = MemoryLocation.valueOf(varName,0);
    } else {
      memoryLocation = MemoryLocation.valueOf(functionName, varName, 0);
    }

    if (addressedVariables.contains(decl.getQualifiedName())
        && declarationType instanceof CType
        && ((CType) declarationType).getCanonicalType() instanceof CPointerType) {
      ValueAnalysisState.addToBlacklist(memoryLocation);
    }

    if (init instanceof AInitializerExpression) {
      ExpressionValueVisitor evv = getVisitor();
      AExpression exp = ((AInitializerExpression) init).getExpression();
      initialValue = getExpressionValue(exp, declarationType, evv);

      if (isMissingCExpressionInformation(evv, exp)) {
        addMissingInformation(memoryLocation, exp);
      }
    }

    if (initialValue.isUnknown() && declarationType instanceof JType) {
      initialValue = getSymbolicIdentifier(declarationType);
    }

    if (isTrackedField(decl, initialValue)) {
      if (missingFieldVariableObject) {
        fieldNameAndInitialValue = Pair.of(varName, initialValue);
      } else if (missingInformationRightJExpression == null) {
        newElement.assignConstant(memoryLocation, initialValue, declarationType);
      } else {
        missingInformationLeftJVariable = memoryLocation.getAsSimpleString();
      }
    } else {

      // If variable not tracked, its Object is irrelevant
      missingFieldVariableObject = false;
      newElement.forget(memoryLocation);
    }

    return newElement;
  }

  private Value getDefaultInitialValue(AVariableDeclaration pDeclaration) {
    final boolean defaultBooleanValue = false;
    final long defaultNumericValue = 0;

    if (pDeclaration.isGlobal()) {
      Type declarationType = pDeclaration.getType();

      if (isComplexJavaType(declarationType)) {
        return NullValue.getInstance();

      } else if (declarationType instanceof JSimpleType) {
        JBasicType basicType = ((JSimpleType) declarationType).getType();

        switch (basicType) {
          case BOOLEAN:
            return BooleanValue.valueOf(defaultBooleanValue);
          case BYTE:
          case CHAR:
          case SHORT:
          case INT:
          case LONG:
          case FLOAT:
          case DOUBLE:
            return new NumericValue(defaultNumericValue);
          case UNSPECIFIED:
            return UnknownValue.getInstance();
          default:
            throw new AssertionError("Impossible type for declaration: " + basicType);
        }
      }
    }

    return UnknownValue.getInstance();
  }

  private boolean isMissingCExpressionInformation(ExpressionValueVisitor pEvv,
      ARightHandSide pExp) {

    return pExp instanceof CExpression && (pEvv.hasMissingPointer());
  }

  private boolean isTrackedField(ADeclaration pDeclaration, Value pInitialValue) {
    boolean isNoComplexType = !isComplexJavaType(pDeclaration.getType())
        && (missingInformationRightJExpression != null || !pInitialValue.isUnknown());

    return isNoComplexType || pInitialValue instanceof EnumConstantValue
        || pInitialValue instanceof ArrayValue || pInitialValue instanceof NullValue
        || pInitialValue.isUnknown();
  }

  private boolean isComplexJavaType(Type pType) {
    return pType instanceof JClassOrInterfaceType
        || pType instanceof JArrayType;
  }

  private Value getSymbolicIdentifier(Type pType) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    return factory.createIdentifier(pType);
  }

  @Override
  protected ValueAnalysisState handleStatementEdge(AStatementEdge cfaEdge, AStatement expression)
    throws UnrecognizedCodeException {

    if (expression instanceof CFunctionCall) {
      CExpression fn = ((CFunctionCall)expression).getFunctionCallExpression().getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression)fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);
        } else if (func.equals("free")) {
          // Needed for erasing values
          missingInformationList.add(new MissingInformation(((CFunctionCall)expression).getFunctionCallExpression()));
        }
      }
    }

    // expression is a binary operation, e.g. a = b;

    if (expression instanceof AAssignment) {
      return handleAssignment((AAssignment)expression, cfaEdge);

    // external function call - do nothing
    } else if (expression instanceof AFunctionCallStatement) {

    // there is such a case
    } else if (expression instanceof AExpressionStatement) {

    } else {
      throw new UnrecognizedCodeException("Unknown statement", cfaEdge, expression);
    }

    return state;
  }

  private ValueAnalysisState handleAssignment(AAssignment assignExpression, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {
    AExpression op1    = assignExpression.getLeftHandSide();
    ARightHandSide op2 = assignExpression.getRightHandSide();

    if (!isTrackedType(op1.getExpressionType())) {
      return state;
    }

    if (op1 instanceof AIdExpression) {
      /*
       * Assignment of the form
       *  a = ...
       */

        if (op1 instanceof JIdExpression && isDynamicField((JIdExpression) op1)) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
        }

        MemoryLocation memloc = getMemoryLocation((AIdExpression) op1);

        return handleAssignmentToVariable(memloc, op1.getExpressionType(), op2, getVisitor());
    } else if (op1 instanceof APointerExpression) {
      // *a = ...

      if (isRelevant(op1, op2)) {
        missingInformationList.add(new MissingInformation(op1, op2));
      }

    } else if (op1 instanceof CFieldReference) {

      ExpressionValueVisitor v = getVisitor();

      MemoryLocation memLoc = v.evaluateMemoryLocation((CFieldReference) op1);

      if (v.hasMissingPointer() && isRelevant(op1, op2)) {
        missingInformationList.add(new MissingInformation(op1, op2));
      }

      if (memLoc != null) {
        return handleAssignmentToVariable(memLoc, op1.getExpressionType(), op2, v);
      }

    } else if (op1 instanceof AArraySubscriptExpression) {
      // array cell
      if (op1 instanceof CArraySubscriptExpression) {

        ExpressionValueVisitor v = getVisitor();

        MemoryLocation memLoc = v.evaluateMemoryLocation((CLeftHandSide) op1);

        if (v.hasMissingPointer() && isRelevant(op1, op2)) {
          missingInformationList.add(new MissingInformation(op1, op2));
        }

        if (memLoc != null) {
          return handleAssignmentToVariable(memLoc, op1.getExpressionType(), op2, v);
        }
      } else if (op1 instanceof JArraySubscriptExpression) {
        JArraySubscriptExpression arrayExpression = (JArraySubscriptExpression) op1;
        ExpressionValueVisitor evv = getVisitor();

        ArrayValue arrayValue = getInnerMostArray(arrayExpression);
        Value subscriptValue = arrayExpression.getSubscriptExpression().accept(evv);
        long index;

        if (arrayValue == null || subscriptValue.isUnknown()) {
          assignUnknownValueToIdentifier((JArraySubscriptExpression) op1);

        } else {
          index = ((NumericValue) subscriptValue).longValue();

          if (index < 0 || index >= arrayValue.getArraySize()) {
            throw new UnrecognizedCodeException("Invalid index " + index + " for array " + arrayValue, cfaEdge);
          }

          // changes array value in old state
          handleAssignmentToArray(arrayValue, (int) index, op2);
          return ValueAnalysisState.copyOf(state);
        }
      }
    } else {
      throw new UnrecognizedCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }

    return state; // the default return-value is the old state
  }

  private boolean isTrackedType(Type pType) {
    if (pType instanceof JType) {
      return trackJavaArrayValues || !(pType instanceof JArrayType);
    } else {
      return true;
    }
  }

  private MemoryLocation getMemoryLocation(AIdExpression pIdExpression) {
    String varName = pIdExpression.getName();

    if (isGlobal(pIdExpression)) {
      return MemoryLocation.valueOf(varName, 0);
    } else {
      return MemoryLocation.valueOf(functionName, varName, 0);
    }
  }

  private boolean isRelevant(AExpression pOp1, ARightHandSide pOp2) {
    return pOp1 instanceof CExpression && pOp2 instanceof CExpression;
  }

  /** This method analyses the expression with the visitor and assigns the value to lParam.
   * The method returns a new state, that contains (a copy of) the old state and the new assignment. */
  private ValueAnalysisState handleAssignmentToVariable(
      MemoryLocation assignedVar, final Type lType, ARightHandSide exp, ExpressionValueVisitor visitor)
      throws UnrecognizedCCodeException {

    Value value;
    if (exp instanceof JRightHandSide) {
       value = visitor.evaluate((JRightHandSide) exp, (JType) lType);
    } else if (exp instanceof CRightHandSide) {
       value = visitor.evaluate((CRightHandSide) exp, (CType) lType);
    } else {
      throw new AssertionError("unknown righthandside-expression: " + exp);
    }

    if (visitor.hasMissingPointer()) {
      assert !value.isExplicitlyKnown();
    }

    if (isMissingCExpressionInformation(visitor, exp)) {
      // Evaluation
      addMissingInformation(assignedVar, exp);
    }

    // here we clone the state, because we get new information or must forget it.
    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);

    if (visitor.hasMissingFieldAccessInformation()) {
      // This may happen if an object of class is created which could not be parsed,
      // In  such a case, forget about it
      if (!value.isUnknown()) {
        newElement.forget(assignedVar);
        return newElement;
      } else {
        missingInformationRightJExpression = (JRightHandSide) exp;
        if (!missingScopedFieldName) {
          missingInformationLeftJVariable = assignedVar.getAsSimpleString();
        }
      }
    }

    if (missingScopedFieldName) {
      notScopedFieldValue = value;
    } else {
      // some heuristics to clear wrong information
      // when a struct or a pointer to one is assigned
      // TODO not implemented in SMG version of ValueAnalysisCPA
//      newElement.forgetAllWithPrefix(assignedVar + ".");
//      newElement.forgetAllWithPrefix(assignedVar + "->");

      // if there is no information left to evaluate but the value is unknown, we assign a symbolic
      // identifier to keep track of the variable.
      if (value.isUnknown() && missingInformationRightJExpression == null) {
        if (lType instanceof JType) {
          value = getSymbolicIdentifier(lType);
        } else {
          newElement.forget(assignedVar);
        }
      }

      if (!value.isUnknown()) {
        newElement.assignConstant(assignedVar, value, lType);
      }
    }

    return newElement;
  }

  private void addMissingInformation(MemoryLocation pMemLoc, ARightHandSide pExp) {
    if (pExp instanceof CExpression) {

      missingInformationList.add(new MissingInformation(pMemLoc,
          (CExpression) pExp));
    }
  }

  private void addMissingInformation(CLeftHandSide pOp1, Value pValue) {
    missingInformationList.add(new MissingInformation(pOp1, pValue));

  }

  private @Nullable ArrayValue getInnerMostArray(JArraySubscriptExpression pArraySubscriptExpression) {
    JExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();

    if (arrayExpression instanceof JIdExpression) {
      Value idValue = getVisitor().evaluateJIdExpression((JIdExpression) arrayExpression);
      if (!idValue.isUnknown()) {
        return (ArrayValue) idValue;
      } else {
        return null;
      }
    } else {
      final JArraySubscriptExpression arraySubscriptExpression = (JArraySubscriptExpression) arrayExpression;
      ArrayValue arrayValue = getInnerMostArray(arraySubscriptExpression);

      // check if we already are at the outermost array
      if (arrayValue != null && arrayValue.getArrayType().getDimensions() > 1) {
        final ExpressionValueVisitor evv = getVisitor();
        final Value indexValue = arraySubscriptExpression.getSubscriptExpression().accept(evv);


        if (indexValue.isUnknown()) {
          return null;
        }

        long index = ((NumericValue) indexValue).longValue();

        if (index >= arrayValue.getArraySize() || index < 0) {
          return null;
        }

        arrayValue = (ArrayValue) arrayValue.getValueAt((int) index);
      }

      return arrayValue;
    }
  }

  private void handleAssignmentToArray(ArrayValue pArray, int index, ARightHandSide exp) {
    assert exp instanceof JExpression;

    pArray.setValue(((JExpression) exp).accept(getVisitor()), index);
  }

  private void assignUnknownValueToIdentifier(JArraySubscriptExpression pArraySubscriptExpression) {
    JExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();

    if (arrayExpression instanceof JIdExpression) {
      JIdExpression idExpression = (JIdExpression) arrayExpression;
      MemoryLocation memLoc = getMemoryLocation(idExpression);

      state.assignConstant(memLoc, Value.UnknownValue.getInstance(), JSimpleType.getUnspecified());
    } else {
      assignUnknownValueToIdentifier((JArraySubscriptExpression) arrayExpression);
    }
  }

  /**
   * Visitor that derives further information from an assume edge
   */
  private class AssigningValueVisitor extends ExpressionValueVisitor {

    private ValueAnalysisState assignableState;

    private Collection<String> booleans;

    protected boolean truthValue = false;

    public AssigningValueVisitor(ValueAnalysisState assignableState, boolean truthValue, Collection<String> booleanVariables) {
      super(state, functionName, machineModel, logger, symbolicValues);
      this.assignableState  = assignableState;
      this.booleans         = booleanVariables;
      this.truthValue       = truthValue;
    }

    private AExpression unwrap(AExpression expression) {
      // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

      if (expression instanceof CCastExpression) {
        CCastExpression exp = (CCastExpression)expression;
        expression = exp.getOperand();

        expression = unwrap(expression);
      }

      return expression;
    }

    @Override
    public Value visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
      BinaryOperator binaryOperator = pE.getOperator();
      CExpression lVarInBinaryExp   = pE.getOperand1();
      CExpression rVarInBinaryExp   = pE.getOperand2();

      lVarInBinaryExp = (CExpression) unwrap(pE.getOperand1());

      Value leftValue   = lVarInBinaryExp.accept(this);
      Value rightValue  = rVarInBinaryExp.accept(this);

      if (isEqualityAssumption(binaryOperator)) {
        if (leftValue.isUnknown() && !rightValue.isUnknown() && isAssignable(lVarInBinaryExp)) {
          assignableState.assignConstant(getMemoryLocation(lVarInBinaryExp), rightValue, pE.getExpressionType());

        } else if (rightValue.isUnknown() && !leftValue.isUnknown() && isAssignable(rVarInBinaryExp)) {
          assignableState.assignConstant(getMemoryLocation(rVarInBinaryExp), leftValue, pE.getExpressionType());
        }
      }

      if (isNonEqualityAssumption(binaryOperator)) {
        if (assumingUnknownToBeZero(leftValue, rightValue) && isAssignable(lVarInBinaryExp)) {
          MemoryLocation leftMemLoc = getMemoryLocation(lVarInBinaryExp);

          if (optimizeBooleanVariables && (booleans.contains(leftMemLoc.getAsSimpleString()) || initAssumptionVars)) {
            assignableState.assignConstant(leftMemLoc, new NumericValue(1L), pE.getExpressionType());
          }

        } else if (optimizeBooleanVariables && (assumingUnknownToBeZero(rightValue, leftValue) && isAssignable(rVarInBinaryExp))) {
          MemoryLocation rightMemLoc = getMemoryLocation(rVarInBinaryExp);

          if (booleans.contains(rightMemLoc.getAsSimpleString()) || initAssumptionVars) {
            assignableState.assignConstant(rightMemLoc, new NumericValue(1L), pE.getExpressionType());
          }
        }
      }

      return super.visit(pE);
    }

    private boolean assumingUnknownToBeZero(Value value1, Value value2) {
      return value1.isUnknown() && value2.equals(new NumericValue(BigInteger.ZERO));
    }

    private boolean isEqualityAssumption(BinaryOperator binaryOperator) {
      return (binaryOperator == BinaryOperator.EQUALS && truthValue)
          || (binaryOperator == BinaryOperator.NOT_EQUALS && !truthValue);
    }

    private boolean isNonEqualityAssumption(BinaryOperator binaryOperator) {
      return (binaryOperator == BinaryOperator.EQUALS && !truthValue)
          || (binaryOperator == BinaryOperator.NOT_EQUALS && truthValue);
    }

    @Override
    public Value visit(JBinaryExpression pE) {
      JBinaryExpression.BinaryOperator binaryOperator = pE.getOperator();

      JExpression lVarInBinaryExp = pE.getOperand1();

      lVarInBinaryExp = (JExpression) unwrap(lVarInBinaryExp);

      JExpression rVarInBinaryExp = pE.getOperand2();

      Value leftValueV = lVarInBinaryExp.accept(this);
      Value rightValueV = rVarInBinaryExp.accept(this);

      if ((binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && truthValue)
          || (binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && !truthValue)) {

        if (leftValueV.isUnknown() && rightValueV.isExplicitlyKnown()
            && isAssignable(lVarInBinaryExp)) {
          assignValueToState((AIdExpression) lVarInBinaryExp, rightValueV);

        } else if (rightValueV.isUnknown() && leftValueV.isExplicitlyKnown()
            && isAssignable(rVarInBinaryExp)) {
          assignValueToState((AIdExpression) rVarInBinaryExp, leftValueV);
        }
      }

      if (initAssumptionVars) {
        // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
        // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
        if ((binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && truthValue)
            || (binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && !truthValue)) {

          if (leftValueV.isUnknown() && rightValueV.isExplicitlyKnown()
              && isAssignable(lVarInBinaryExp)) {

            // we only want BooleanValue objects for boolean values in the future
            assert rightValueV instanceof BooleanValue;
            BooleanValue booleanValueRight = BooleanValue.valueOf(rightValueV).get();

            if (!booleanValueRight.isTrue()) {
              assignValueToState((AIdExpression) lVarInBinaryExp, BooleanValue.valueOf(true));
            }

          } else if (rightValueV.isUnknown() && leftValueV.isExplicitlyKnown()
              && isAssignable(rVarInBinaryExp)) {

            // we only want BooleanValue objects for boolean values in the future
            assert leftValueV instanceof BooleanValue;
            BooleanValue booleanValueLeft = BooleanValue.valueOf(leftValueV).get();

            if (!booleanValueLeft.isTrue()) {
              assignValueToState((AIdExpression) rVarInBinaryExp, BooleanValue.valueOf(true));
            }
          }
        }
      }
      return super.visit(pE);
    }

    // Assign the given value of the given IdExpression to the state of this TransferRelation
    private void assignValueToState(AIdExpression pIdExpression, Value pValue) {
      ASimpleDeclaration declaration = pIdExpression.getDeclaration();

      if (declaration != null) {
        assignableState.assignConstant(declaration.getQualifiedName(), pValue);
      } else {
        MemoryLocation memLoc = MemoryLocation.valueOf(getFunctionName(), pIdExpression.getName(),
            0);
        assignableState.assignConstant(memLoc, pValue, pIdExpression.getExpressionType());
      }
    }

    protected MemoryLocation getMemoryLocation(CExpression pLValue) throws UnrecognizedCCodeException {
      ExpressionValueVisitor v = getVisitor();
      assert pLValue instanceof CLeftHandSide;
      return checkNotNull(v.evaluateMemoryLocation(pLValue));
    }

    protected boolean isAssignable(JExpression expression) {

      boolean result = false;

      if (expression instanceof JIdExpression) {
        JSimpleDeclaration decl = ((JIdExpression) expression).getDeclaration();

        if (decl == null) {
          result = false;
        } else if (decl instanceof JFieldDeclaration) {
          result = ((JFieldDeclaration) decl).isStatic();
        } else {
          result = true;
        }
      }

      return result;
    }



    protected boolean isAssignable(CExpression expression) throws UnrecognizedCCodeException  {

      if (expression instanceof CIdExpression) {
        return true;
      }

      if (expression instanceof CFieldReference || expression instanceof CArraySubscriptExpression) {
        ExpressionValueVisitor evv = getVisitor();
        return evv.canBeEvaluated(expression);
      }

      return false;
    }
  }


  private class SMGAssigningValueVisitor extends AssigningValueVisitor {

    private final ValueAnalysisSMGCommunicator expressionEvaluator;
    @SuppressWarnings("unused")
    private final SMGState smgState;

    public SMGAssigningValueVisitor(
        ValueAnalysisState pAssignableState,
        boolean pTruthValue,
        Collection<String> booleanVariables,
        SMGState pSmgState) {

      super(pAssignableState, pTruthValue, booleanVariables);
      checkNotNull(pSmgState);
      expressionEvaluator = new ValueAnalysisSMGCommunicator(pAssignableState, functionName,
          pSmgState, machineModel, logger, edge);
      smgState = pSmgState;
    }

    @Override
    protected boolean isAssignable(CExpression pExpression) throws UnrecognizedCCodeException {

      //TODO Ugly, Refactor
      if (pExpression instanceof CLeftHandSide) {
        MemoryLocation memLoc =
            expressionEvaluator.evaluateLeftHandSide(pExpression);

        return memLoc != null;
      }

      return false;
    }

    @Override
    protected MemoryLocation getMemoryLocation(CExpression pLValue) throws UnrecognizedCCodeException {
      return expressionEvaluator.evaluateLeftHandSide(pLValue);
    }
  }

  private class  FieldAccessExpressionValueVisitor extends ExpressionValueVisitor {
    private final RTTState jortState;

    public FieldAccessExpressionValueVisitor(RTTState pJortState) {
      super(state, functionName, machineModel, logger, symbolicValues);
      jortState = pJortState;
    }

    @Override
    public Value visit(JBinaryExpression binaryExpression) {
      return super.visit(binaryExpression);
    }

    private String handleIdExpression(JIdExpression expr) {

      JSimpleDeclaration decl = expr.getDeclaration();

      if (decl == null) {
        return null;
      }

      String objectScope = getObjectScope(jortState, functionName, expr);

      return getRTTScopedVariableName(decl, functionName, objectScope);

    }

    @Override
    public Value visit(JIdExpression idExp) {

      String varName = handleIdExpression(idExp);

      if (state.contains(varName)) {
        return state.getValueFor(varName);
      } else {
        return Value.UnknownValue.getInstance();
      }
    }
  }

  private Value getExpressionValue(AExpression expression, final Type type, ExpressionValueVisitor evv)
      throws UnrecognizedCCodeException {
    if (!isTrackedType(type)) {
      return UnknownValue.getInstance();
    }

    if (expression instanceof JRightHandSide) {

      final Value value = evv.evaluate((JRightHandSide) expression, (JType) type);

      if (evv.hasMissingFieldAccessInformation()) {
        missingInformationRightJExpression = (JRightHandSide) expression;
        return Value.UnknownValue.getInstance();
      } else {
        return value;
      }
    } else if (expression instanceof CRightHandSide) {
      return evv.evaluate((CRightHandSide) expression, (CType) type);
    } else {
      throw new AssertionError("unhandled righthandside-expression: " + expression);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge, Precision precision)
    throws CPATransferException {
    assert element instanceof ValueAnalysisState;

    ArrayList<ValueAnalysisState> toStrengthen = new ArrayList<>();
    ArrayList<ValueAnalysisState> result = new ArrayList<>();
    toStrengthen.add((ValueAnalysisState) element);
    result.add((ValueAnalysisState) element);

    for (AbstractState ae : elements) {
      if (ae instanceof RTTState) {
        result.clear();
        for (ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          Collection<ValueAnalysisState> ret = strengthen((RTTState)ae);
          if (ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if (ae instanceof SMGState) {
        result.clear();
        for (ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          Collection<ValueAnalysisState> ret = strengthen((SMGState)ae);
          if (ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if (ae instanceof AutomatonState) {
        result.clear();
        for (ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          AutomatonState autoState = (AutomatonState) ae;
          Collection<ValueAnalysisState> ret = automatonAssumesAsStatements ?
              strengthenAutomatonStatement(autoState, state, cfaEdge) : strengthenAutomatonAssume(autoState, state, cfaEdge);
          if (ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      }
    }

    // Do post processing
    final Collection<AbstractState> postProcessedResult = new ArrayList<>(result.size());
    for (ValueAnalysisState rawResult : result) {
      // The original state has already been post-processed
      if (rawResult == element) {
        postProcessedResult.add(element);
      } else {
        postProcessedResult.addAll(postProcessing(rawResult));
      }
    }

    super.resetInfo();
    oldState = null;

    return postProcessedResult;
  }

  private Collection<ValueAnalysisState> strengthenAutomatonStatement(AutomatonState pAutomatonState, ValueAnalysisState pState, CFAEdge pCfaEdge) throws CPATransferException {

    List<CStatementEdge> statementEdges = pAutomatonState.getAsStatementEdges(pCfaEdge.getPredecessor().getFunctionName());

    ValueAnalysisState state = pState;

    for (CStatementEdge stmtEdge : statementEdges) {
      state = handleStatementEdge((AStatementEdge)stmtEdge, (AStatement)stmtEdge.getStatement());

      if (state == null) {
        break;
      } else {
        setInfo(state, precision, pCfaEdge);
      }
    }

    if (state == null) {
      return Collections.emptyList();
    } else {
      return Collections.singleton(state);
    }
  }

  private Collection<ValueAnalysisState> strengthenAutomatonAssume(AutomatonState pAutomatonState, ValueAnalysisState pState, CFAEdge pCfaEdge) throws CPATransferException {

    List<AssumeEdge> assumeEdges = pAutomatonState.getAsAssumeEdges(pCfaEdge.getPredecessor().getFunctionName());

    ValueAnalysisState state = pState;


    for (AssumeEdge assumeEdge : assumeEdges) {
      state = this.handleAssumption(assumeEdge, assumeEdge.getExpression(), assumeEdge.getTruthAssumption());

      if (state == null) {
        break;
      } else {
        setInfo(state, precision, pCfaEdge);
      }
    }

    if (state == null) {
      return Collections.emptyList();
    } else {
      return Collections.singleton(state);
    }
  }

  private Collection<ValueAnalysisState> strengthen(SMGState smgState) throws UnrecognizedCCodeException {

    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);

    //TODO Refactor

    for (MissingInformation missingInformation : missingInformationList) {
      if (missingInformation.isMissingAssumption()) {
        newElement = resolvingAssumption(newElement, smgState, missingInformation);
      } else if (missingInformation.isMissingAssignment()) {
        if (isRelevant(missingInformation)) {
          newElement = resolvingAssignment(newElement, smgState, missingInformation);
        } else {
          // We have to forget Nonrelevant Information to not contradict SMGState.
          newElement = forgetMemLoc(newElement, missingInformation, smgState);
        }
      } else if (missingInformation.isFreeInvocation()) {
        newElement = resolveFree(newElement, smgState, missingInformation);
      }
    }

    //TODO More common handling of missing information (erase missing Information if other cpas solved it).
    missingInformationList.clear();

    if (newElement == null) {
      return new HashSet<>();
    }

    return state.equals(newElement) ? null : Collections.singleton(newElement);
  }

  private ValueAnalysisState resolveFree(ValueAnalysisState pNewElement, SMGState pSmgState,
      MissingInformation pMissingInformation) throws UnrecognizedCCodeException {

    CFunctionCallExpression functionCall = pMissingInformation.getMissingFreeInvocation();

    CExpression pointerExp;

    try {
      pointerExp = functionCall.getParameterExpressions().get(0);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Bulit in function free has no parameter", edge, functionCall);
    }

    ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(pNewElement, functionName, pSmgState,
        machineModel, logger, edge);

    SMGAddressValue address;
    try {
      address = cc.evaluateSMGAddressExpression(pointerExp);
    } catch (CPATransferException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Error while evaluating free pointer exception.", edge, functionCall);
    }

    if (address.isUnknown()) {
      //TODO if sound Option is implemented, here every heap value has to be erased.
      return pNewElement;
    }

    pNewElement.forgetValuesWithIdentifier(address.getObject().getLabel());

    return pNewElement;
  }

  private ValueAnalysisState forgetMemLoc(ValueAnalysisState pNewElement, MissingInformation pMissingInformation,
      SMGState pSmgState) throws UnrecognizedCCodeException {

    MemoryLocation memoryLocation = null;

    if (pMissingInformation.hasKnownMemoryLocation()) {
      memoryLocation = pMissingInformation.getcLeftMemoryLocation();
    } else if (pMissingInformation.hasUnknownMemoryLocation()) {
      memoryLocation = resolveMemoryLocation(pSmgState,
          pMissingInformation.getMissingCLeftMemoryLocation());
    }

    if (memoryLocation == null) {
      // Always return the new Element
      // if you want to interrupt the calculation
      // in case it was changed before
      return pNewElement;
    } else {
      pNewElement.forget(memoryLocation);
      return pNewElement;
    }
  }

  private boolean isRelevant(MissingInformation missingInformation) {

    CRightHandSide value;

    if (missingInformation.hasUnknownMemoryLocation()) {
      value = missingInformation.getMissingCLeftMemoryLocation();
    } else if (missingInformation.hasUnknownValue()) {
      value = missingInformation.getMissingCExpressionInformation();
    } else {
      return false;
    }

    CType type = value.getExpressionType().getCanonicalType();

    return !(type instanceof CPointerType);
  }

  //TODO Better Name, these are not just Assignments, but also calls, etc
  private ValueAnalysisState resolvingAssignment(ValueAnalysisState pNewElement,
      SMGState pSmgState, MissingInformation pMissingInformation) throws UnrecognizedCCodeException {

    MemoryLocation memoryLocation = null;

    if (pMissingInformation.hasKnownMemoryLocation()) {
      memoryLocation = pMissingInformation.getcLeftMemoryLocation();
    } else if (pMissingInformation.hasUnknownMemoryLocation()) {
      memoryLocation = resolveMemoryLocation(pSmgState,
          pMissingInformation.getMissingCLeftMemoryLocation());
    }

    if (memoryLocation == null) {
      // Always return the new Element
      // if you want to interrupt the calculation
      // in case it was changed before
      return pNewElement;
    }

    Value value = Value.UnknownValue.getInstance();

    if (pMissingInformation.hasKnownValue()) {
      value = pMissingInformation.getcExpressionValue();
    } else if (pMissingInformation.hasUnknownValue()) {
      value = resolveValue(pSmgState, pMissingInformation.getMissingCExpressionInformation());
    }

    if (value.isUnknown()) {
      // Always return the new Element
      // if you want to interrupt the calculation
      // in case it was changed before
      if (pNewElement.contains(memoryLocation)) {
        pNewElement.forget(memoryLocation);
      }
      return pNewElement;
    }

    pNewElement.assignConstant(memoryLocation, value, pNewElement.getTypeForMemoryLocation(memoryLocation));

    return pNewElement;
  }

  private Value resolveValue(SMGState pSmgState, CExpression rValue)
      throws UnrecognizedCCodeException {

    ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(oldState, functionName,
        pSmgState, machineModel, logger, edge);

    return cc.evaluateExpression(rValue);
  }

  private MemoryLocation resolveMemoryLocation(SMGState pSmgState, CExpression lValue)
      throws UnrecognizedCCodeException {

    ValueAnalysisSMGCommunicator cc =
        new ValueAnalysisSMGCommunicator(oldState, functionName, pSmgState, machineModel, logger, edge);

    return cc.evaluateLeftHandSide(lValue);
  }

  private ValueAnalysisState resolvingAssumption(ValueAnalysisState pNewElement,
      SMGState pSmgState, MissingInformation pMissingInformation) throws UnrecognizedCCodeException {

    Boolean bTruthValue = pMissingInformation.getTruthAssumption();

    long truthValue = bTruthValue ? 1 : 0;

    Value value = resolveValue(pSmgState, pMissingInformation.getMissingCExpressionInformation());

    if (value.isExplicitlyKnown() && !value.equals(new NumericValue(truthValue))) {
      return null;
    } else {

      if (!value.isExplicitlyKnown()) {

        // Try deriving further Information
        ValueAnalysisState element = ValueAnalysisState.copyOf(pNewElement);
        SMGAssigningValueVisitor avv = new SMGAssigningValueVisitor(element, bTruthValue, booleanVariables, pSmgState);
        pMissingInformation.getMissingCExpressionInformation().accept(avv);

        return element;
      }

      return pNewElement;
    }
  }

  private Collection<ValueAnalysisState> strengthen(RTTState rttState)
      throws UnrecognizedCCodeException {

    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);

    if (missingFieldVariableObject) {
      newElement.assignConstant(getRTTScopedVariableName(
          fieldNameAndInitialValue.getFirst(),
          rttState.getKeywordThisUniqueObject()),
          fieldNameAndInitialValue.getSecond());

      missingFieldVariableObject = false;
      fieldNameAndInitialValue = null;
      return Collections.singleton(newElement);

    } else if (missingScopedFieldName) {

      newElement = handleNotScopedVariable(rttState, newElement);
      missingScopedFieldName = false;
      notScopedField = null;
      notScopedFieldValue = null;
      missingInformationRightJExpression = null;

      if (newElement != null) {
      return Collections.singleton(newElement);
      } else {
        return null;
      }
    } else if (missingAssumeInformation && missingInformationRightJExpression != null) {
      Value value = handleMissingInformationRightJExpression(rttState);

      missingAssumeInformation = false;
      missingInformationRightJExpression = null;

      if (value == null) {
        return null;
      } else if ((((AssumeEdge) edge).getTruthAssumption() && value.equals(new NumericValue(1L)))
          || (!((AssumeEdge) edge).getTruthAssumption() && value.equals(new NumericValue(1L)))) {
        return Collections.singleton(newElement);
      } else {
        return new HashSet<>();
      }
    } else if (missingInformationRightJExpression != null) {

      Value value = handleMissingInformationRightJExpression(rttState);

      if (value.isExplicitlyKnown()) {
        newElement.assignConstant(missingInformationLeftJVariable, value);
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        return Collections.singleton(newElement);
      } else {
        if (missingInformationLeftJVariable != null) {
          newElement.forget(missingInformationLeftJVariable);
        }
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        return Collections.singleton(newElement);
      }
    }
    return null;
  }

  private String getRTTScopedVariableName(String fieldName, String uniqueObject) {
    return  uniqueObject + "::"+ fieldName;
  }

  private Value handleMissingInformationRightJExpression(RTTState pJortState)
      throws UnrecognizedCCodeException {
    return missingInformationRightJExpression.accept(
        new FieldAccessExpressionValueVisitor(pJortState));
  }

  private ValueAnalysisState handleNotScopedVariable(RTTState rttState, ValueAnalysisState newElement) throws UnrecognizedCCodeException {

   String objectScope = getObjectScope(rttState, functionName, notScopedField);

   if (objectScope != null) {

     String scopedFieldName = getRTTScopedVariableName(notScopedField.getName(), objectScope);

     Value value = notScopedFieldValue;
     if (missingInformationRightJExpression != null) {
       value = handleMissingInformationRightJExpression(rttState);
     }

     if (!value.isUnknown()) {
       newElement.assignConstant(scopedFieldName, value);
       return newElement;
     } else {
       newElement.forget(scopedFieldName);
       return newElement;
     }
   } else {
     return null;
   }


  }

  private String getObjectScope(RTTState rttState, String methodName,
      JIdExpression notScopedField) {

    // Could not resolve var
    if (notScopedField.getDeclaration() == null) {
      return null;
    }

    if (notScopedField instanceof JFieldAccess) {

      JIdExpression qualifier = ((JFieldAccess) notScopedField).getReferencedVariable();

      String qualifierScope = getObjectScope(rttState, methodName, qualifier);

      String scopedFieldName =
          getRTTScopedVariableName(qualifier.getDeclaration(), methodName, qualifierScope);

      if (rttState.contains(scopedFieldName)) {
        return rttState.getUniqueObjectFor(scopedFieldName);
      } else {
        return null;
      }
    } else {
      if (rttState.contains(RTTState.KEYWORD_THIS)) {
        return rttState.getUniqueObjectFor(RTTState.KEYWORD_THIS);
      } else {
        return null;
      }
    }
  }

  private String getRTTScopedVariableName(
      JSimpleDeclaration decl,
      String methodName, String uniqueObject) {

    if (decl == null) { return ""; }

    if (decl instanceof JFieldDeclaration && ((JFieldDeclaration) decl).isStatic()) {
      return decl.getName();
    } else if (decl instanceof JFieldDeclaration) {
      return uniqueObject + "::" + decl.getName();
    } else {
      return methodName + "::" + decl.getName();
    }
  }

  private static class MissingInformation {

    /**
     * This field stores the Expression of the Memory Location that
     * could not be evaluated.
     */
    private final CExpression missingCLeftMemoryLocation;

    /**
     *  This expression stores the Memory Location
     *  to be assigned.
     */
    private final MemoryLocation cLeftMemoryLocation;

    /**
     * Expression could not be evaluated due to missing information. (e.g.
     * missing pointer alias).
     */
    private final CExpression missingCExpressionInformation;

    /**
     * Expression could not be evaluated due to missing information. (e.g.
     * missing pointer alias).
     */
    private final Value cExpressionValue;

    /**
     * The truth Assumption made in this assume edge.
     */
    private final Boolean truthAssumption;

    private CFunctionCallExpression missingFreeInvocation = null;

    @SuppressWarnings("unused")
    public MissingInformation(CExpression pMissingCLeftMemoryLocation,
        CExpression pMissingCExpressionInformation) {
      missingCExpressionInformation = pMissingCExpressionInformation;
      missingCLeftMemoryLocation = pMissingCLeftMemoryLocation;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = null;
    }

    //TODO Better checks...don't be lazy, just because class
    // will likely change.

    public boolean hasUnknownValue() {
      return missingCExpressionInformation != null;
    }

    public boolean hasKnownValue() {
      return cExpressionValue != null;
    }

    public boolean hasUnknownMemoryLocation() {
      return missingCLeftMemoryLocation != null;
    }

    public boolean hasKnownMemoryLocation() {
      return cLeftMemoryLocation != null;
    }

    public boolean isMissingAssignment() {
      // TODO Better Name for this method.
      // Checks if a variable needs to be assigned a value,
      // but to evaluate the MemoryLocation, or the value,
      // we lack information.

      return (missingCExpressionInformation != null
              || missingCLeftMemoryLocation != null)
          && truthAssumption == null;
    }

    public boolean isMissingAssumption() {
      return truthAssumption != null && missingCExpressionInformation != null;
    }

    public MissingInformation(CExpression pMissingCLeftMemoryLocation,
        Value pCExpressionValue) {
      missingCExpressionInformation = null;
      missingCLeftMemoryLocation = pMissingCLeftMemoryLocation;
      cExpressionValue = pCExpressionValue;
      cLeftMemoryLocation = null;
      truthAssumption = null;
    }

    public MissingInformation(MemoryLocation pCLeftMemoryLocation,
        CExpression pMissingCExpressionInformation) {
      missingCExpressionInformation = pMissingCExpressionInformation;
      missingCLeftMemoryLocation = null;
      cExpressionValue = null;
      cLeftMemoryLocation = pCLeftMemoryLocation;
      truthAssumption = null;
    }

    public MissingInformation(AExpression pMissingCLeftMemoryLocation,
        ARightHandSide pMissingCExpressionInformation) {
      // This constructor casts to CExpression, just to have as few
      // as possible pieces of code for communication cluttering
      // up the transfer relation.
      // Especially, since this class will later be used to
      // communicate missing Information independent of language

      missingCExpressionInformation = (CExpression) pMissingCExpressionInformation;
      missingCLeftMemoryLocation = (CExpression) pMissingCLeftMemoryLocation;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = null;
    }

    public MissingInformation(Boolean pTruthAssumption,
        ARightHandSide pMissingCExpressionInformation) {
      // This constructor casts to CExpression, just to have as few
      // as possible pieces of code for communication cluttering
      // up the transfer relation.
      // Especially, since this class will later be used to
      // communicate missing Information independent of language

      missingCExpressionInformation = (CExpression) pMissingCExpressionInformation;
      missingCLeftMemoryLocation = null;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = pTruthAssumption;
    }

    public MissingInformation(CFunctionCallExpression pFunctionCallExpression) {
      missingFreeInvocation = pFunctionCallExpression;
      missingCExpressionInformation = null;
      missingCLeftMemoryLocation = null;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = null;

    }

    public boolean isFreeInvocation() {
      return missingFreeInvocation != null;
    }

    public Value getcExpressionValue() {
      checkNotNull(cExpressionValue);
      return cExpressionValue;
    }

    public MemoryLocation getcLeftMemoryLocation() {
      checkNotNull(cLeftMemoryLocation);
      return cLeftMemoryLocation;
    }

    @SuppressWarnings("unused")
    public CExpression getMissingCExpressionInformation() {
      checkNotNull(missingCExpressionInformation);
      return missingCExpressionInformation;
    }

    @SuppressWarnings("unused")
    public CExpression getMissingCLeftMemoryLocation() {
      checkNotNull(missingCLeftMemoryLocation);
      return missingCLeftMemoryLocation;
    }

    @SuppressWarnings("unused")
    public Boolean getTruthAssumption() {
      checkNotNull(truthAssumption);
      return truthAssumption;
    }

    public CFunctionCallExpression getMissingFreeInvocation() {
      return missingFreeInvocation;
    }
  }

  /** returns an initialized, empty visitor */
  private ExpressionValueVisitor getVisitor() {
    return new ExpressionValueVisitor(state, functionName, machineModel, logger, symbolicValues);
  }
}
