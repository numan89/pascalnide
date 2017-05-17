package com.js.interpreter.ast.instructions;

import com.duy.pascal.backend.debugable.DebuggableExecutable;
import com.duy.pascal.backend.linenumber.LineInfo;
import com.js.interpreter.ast.expressioncontext.CompileTimeContext;
import com.js.interpreter.runtime.VariableContext;
import com.js.interpreter.runtime.codeunit.RuntimeExecutable;
import com.js.interpreter.runtime.exception.RuntimePascalException;

public class BreakInstruction extends DebuggableExecutable {
    private LineInfo line;

    public BreakInstruction(LineInfo line) {
        this.line = line;
    }

    @Override
    public LineInfo getLineNumber() {
        return line;
    }

    @Override
    public ExecutionResult executeImpl(VariableContext context, RuntimeExecutable<?> main)
            throws RuntimePascalException {
        return ExecutionResult.BREAK;
    }

    @Override
    public Executable compileTimeConstantTransform(CompileTimeContext c) {
        return this;
    }

}