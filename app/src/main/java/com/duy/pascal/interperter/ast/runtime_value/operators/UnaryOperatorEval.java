/*
 *  Copyright (c) 2017 Tran Le Duy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duy.pascal.interperter.ast.runtime_value.operators;


import android.support.annotation.NonNull;

import com.duy.pascal.interperter.ast.codeunit.RuntimeExecutableCodeUnit;
import com.duy.pascal.interperter.ast.expressioncontext.CompileTimeContext;
import com.duy.pascal.interperter.ast.expressioncontext.ExpressionContext;
import com.duy.pascal.interperter.ast.variablecontext.VariableContext;
import com.duy.pascal.interperter.ast.runtime_value.operators.number.BoolUniOperatorEval;
import com.duy.pascal.interperter.ast.runtime_value.operators.number.DoubleUniOperatorEval;
import com.duy.pascal.interperter.ast.runtime_value.operators.number.IntegerUniOperatorEval;
import com.duy.pascal.interperter.ast.runtime_value.operators.number.LongUniOperatorEval;
import com.duy.pascal.interperter.ast.runtime_value.operators.pointer.AddressEval;
import com.duy.pascal.interperter.ast.runtime_value.operators.pointer.DerefEval;
import com.duy.pascal.interperter.ast.runtime_value.value.AssignableValue;
import com.duy.pascal.interperter.ast.runtime_value.value.RuntimeValue;
import com.duy.pascal.interperter.debugable.DebuggableReturnValue;
import com.duy.pascal.interperter.linenumber.LineInfo;
import com.duy.pascal.interperter.parse_exception.ParsingException;
import com.duy.pascal.interperter.parse_exception.operator.BadOperationTypeException;
import com.duy.pascal.interperter.parse_exception.operator.ConstantCalculationException;
import com.duy.pascal.interperter.runtime_exception.PascalArithmeticException;
import com.duy.pascal.interperter.runtime_exception.RuntimePascalException;
import com.duy.pascal.interperter.runtime_exception.internal.InternalInterpreterException;
import com.duy.pascal.interperter.declaration.lang.types.BasicType;
import com.duy.pascal.interperter.declaration.lang.types.Type;
import com.duy.pascal.interperter.declaration.lang.types.OperatorTypes;
import com.duy.pascal.interperter.declaration.lang.types.PointerType;
import com.duy.pascal.interperter.declaration.lang.types.RuntimeType;

public abstract class UnaryOperatorEval extends DebuggableReturnValue {
    public OperatorTypes operator;
    public RuntimeType type;
    public RuntimeValue operon;
    public LineInfo line;

    protected UnaryOperatorEval(RuntimeValue operon, OperatorTypes operator,
                                LineInfo line) {
        this.operator = operator;
        this.line = line;
        this.operon = operon;
    }

    public static RuntimeValue generateOp(ExpressionContext f,
                                          RuntimeValue v1, OperatorTypes op_type,
                                          LineInfo line) throws ParsingException {
        Type t1 = v1.getRuntimeType(f).declType;

        if (!op_type.canBeUnary) {
            throw new BadOperationTypeException(line, t1, v1, op_type);
        }
        if (op_type == OperatorTypes.ADDRESS) {
            AssignableValue target = v1.asAssignableValue(f);
            if (target != null) {
                return new AddressEval(target, line);
            }
        }
        if (op_type == OperatorTypes.DEREF) {
            if (t1 instanceof PointerType) {
                return new DerefEval(v1, line);
            }
        }
        if (op_type == OperatorTypes.NOT && t1.equals(BasicType.Boolean)) {
            return new BoolUniOperatorEval(v1, op_type, line);
        }
        if (t1.equals(BasicType.Integer)) {
            return new IntegerUniOperatorEval(v1, op_type, line);
        }
        if (t1.equals(BasicType.Long)) {
            return new LongUniOperatorEval(v1, op_type, line);
        }
        if (t1 == BasicType.Double) {
            return new DoubleUniOperatorEval(v1, op_type, line);
        }

        throw new BadOperationTypeException(line, t1, v1, op_type);
    }

    @NonNull
    @Override
    public LineInfo getLineNumber() {
        return line;
    }

    @Override
    public void setLineNumber(LineInfo lineNumber) {
        this.line = lineNumber;
    }

    @Override
    public Object getValueImpl(VariableContext f, RuntimeExecutableCodeUnit<?> main)
            throws RuntimePascalException {
        boolean debug = main.isDebug();
        if (debug) {
            main.getDebugListener().onEvaluatingExpr(line, toString());
            main.setDebug(false);
        }

        Object value = operon.getValue(f, main);
        Object result = operate(value);

        //restore mode
        main.setDebug(debug);
        if (main.isDebug()) {
            main.getDebugListener().onEvaluatedExpr(line, toString(), result.toString());
        }
        return result;
    }

    public abstract Object operate(Object value) throws PascalArithmeticException, InternalInterpreterException;

    @Override
    public String toString() {
        return operator + "" + operon;
    }

    @Override
    public RuntimeType getRuntimeType(ExpressionContext f) throws ParsingException {
        return operon.getRuntimeType(f);
    }

    @Override
    public Object compileTimeValue(CompileTimeContext context)
            throws ParsingException {
        Object value = operon.compileTimeValue(context);
        if (value == null) {
            return null;
        }
        try {
            return operate(value);
        } catch (PascalArithmeticException | InternalInterpreterException e) {
            throw new ConstantCalculationException(e);
        }
    }

    @Override
    public boolean canDebug() {
        return true;
    }
}
