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

package com.duy.pascal.backend.pascaltypes.enumtype;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.duy.pascal.backend.exceptions.ParsingException;
import com.duy.pascal.backend.linenumber.LineInfo;
import com.duy.pascal.backend.pascaltypes.RuntimeType;
import com.js.interpreter.codeunit.RuntimeExecutableCodeUnit;
import com.js.interpreter.expressioncontext.CompileTimeContext;
import com.js.interpreter.expressioncontext.ExpressionContext;
import com.js.interpreter.runtime.VariableContext;
import com.js.interpreter.runtime.exception.RuntimePascalException;
import com.js.interpreter.runtime_value.AssignableValue;
import com.js.interpreter.runtime_value.RuntimeValue;

/**
 * Created by Duy on 25-May-17.
 */

public class EnumElementValue implements RuntimeValue {
    private String name;
    private EnumGroupType type;
    private Object value;
    @Nullable
    private LineInfo lineInfo;

    public EnumElementValue(String name, @NonNull EnumGroupType type, @Nullable Object value, @Nullable LineInfo lineInfo) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.lineInfo = lineInfo;
    }

    @Nullable
    @Override
    public RuntimeValue[] getOutputFormat() {
        return new RuntimeValue[0];
    }

    @Override
    public void setOutputFormat(@Nullable RuntimeValue[] formatInfo) {

    }

    @Override
    public Object getValue(VariableContext f, RuntimeExecutableCodeUnit<?> main) throws RuntimePascalException {
        return value;
    }

    @Override
    public RuntimeType getType(ExpressionContext f) throws ParsingException {
        return new RuntimeType(type, false);
    }

    @Override
    public LineInfo getLineNumber() {
        return lineInfo;
    }

    @Nullable
    @Override
    public Object compileTimeValue(CompileTimeContext context) throws ParsingException {
        return value;
    }

    @Override
    public RuntimeValue compileTimeExpressionFold(CompileTimeContext context) throws ParsingException {
        return null;
    }

    @Override
    public AssignableValue asAssignableValue(ExpressionContext f) {
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * uses for print to console
     */
    @Override
    public String toString() {
        return name;
    }
}
