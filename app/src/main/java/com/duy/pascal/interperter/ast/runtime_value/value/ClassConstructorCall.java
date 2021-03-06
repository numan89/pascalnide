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

package com.duy.pascal.interperter.ast.runtime_value.value;

import android.support.annotation.NonNull;

import com.duy.pascal.interperter.ast.codeunit.RuntimeExecutableCodeUnit;
import com.duy.pascal.interperter.declaration.classunit.ClassConstructor;
import com.duy.pascal.interperter.ast.expressioncontext.CompileTimeContext;
import com.duy.pascal.interperter.ast.expressioncontext.ExpressionContext;
import com.duy.pascal.interperter.ast.instructions.Executable;
import com.duy.pascal.interperter.ast.variablecontext.VariableContext;
import com.duy.pascal.interperter.linenumber.LineInfo;
import com.duy.pascal.interperter.parse_exception.ParsingException;
import com.duy.pascal.interperter.runtime_exception.RuntimePascalException;
import com.duy.pascal.interperter.runtime_exception.internal.MethodReflectionException;
import com.duy.pascal.interperter.declaration.lang.types.ArgumentType;
import com.duy.pascal.interperter.declaration.lang.types.RuntimeType;
import com.duy.pascal.interperter.utils.ArrayUtil;
import com.duy.pascal.frontend.debug.DebugManager;

/**
 * Created by Duy on 17-Jun-17.
 */

public class ClassConstructorCall extends FunctionCall {
    private ClassConstructor constructor;
    private String idName;
    private LineInfo line;

    public ClassConstructorCall(ClassConstructor constructor,
                                RuntimeValue[] arguments, LineInfo line) {
        this.constructor = constructor;
        if (constructor == null) {
            System.err.println("Warning: Null constructor call");
        }
        this.arguments = arguments;
        this.line = line;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    @Override
    public Object getValueImpl(@NonNull VariableContext f, @NonNull RuntimeExecutableCodeUnit<?> main)
            throws RuntimePascalException {
        if (main.isDebug()) {
            main.getDebugListener().onLine((Executable) this, line);
        }
        main.incStack(line);
        //Do not enable debug in any case, because you will need to get value of list parameter,
        //In the case of empty parameters, pause once
        main.scriptControlCheck(line, false);

        //array store value of parameters
        Object[] values = new Object[arguments.length];
        //list type of parameters
        ArgumentType[] argumentTypes = constructor.argumentTypes();

        for (int i = 0; i < values.length; i++) {
            values[i] = arguments[i].getValue(f, main);
        }

        if (main.isDebug()) {
            if (arguments.length > 0) {
                DebugManager.showMessage(arguments[0].getLineNumber(),
                        ArrayUtil.paramsToString(arguments, values), main);
            }
            main.scriptControlCheck(line);

        }
        Object result;
        try {
            result = constructor.call(main, values, idName);

            DebugManager.onFunctionCalled(constructor, arguments, result, main);//debug
        } catch (IllegalArgumentException e) {
            throw new MethodReflectionException(line, e);
        }
        main.decStack();
        if (result == null) {
            result = NullValue.get();
        }
        return result;
    }

    @Override
    public RuntimeType getRuntimeType(ExpressionContext f) {
        return new RuntimeType(constructor.returnType(), false);
    }

    @NonNull
    @Override
    public LineInfo getLineNumber() {
        return line;
    }

    @Override
    public void setLineNumber(LineInfo lineNumber) {

    }

    @Override
    protected String getFunctionName() {
        return constructor.getName();
    }

    @Override
    public RuntimeValue compileTimeExpressionFold(CompileTimeContext context)
            throws ParsingException {
        return new ClassConstructorCall(constructor, compileTimeExpressionFoldArguments(context), line);
    }

    @Override
    public Object compileTimeValue(CompileTimeContext context) throws ParsingException {
        return null;
    }

    @Override
    public Executable compileTimeConstantTransform(CompileTimeContext c)
            throws ParsingException {
        return new SimpleFunctionCall(constructor, compileTimeExpressionFoldArguments(c), line);
    }
}
