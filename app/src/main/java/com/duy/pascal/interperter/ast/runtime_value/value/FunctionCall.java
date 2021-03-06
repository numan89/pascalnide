package com.duy.pascal.interperter.ast.runtime_value.value;

import android.support.annotation.NonNull;

import com.duy.pascal.interperter.declaration.lang.function.AbstractFunction;
import com.duy.pascal.interperter.ast.codeunit.RuntimeExecutableCodeUnit;
import com.duy.pascal.interperter.ast.expressioncontext.CompileTimeContext;
import com.duy.pascal.interperter.ast.expressioncontext.ExpressionContext;
import com.duy.pascal.interperter.ast.instructions.ExecutionResult;
import com.duy.pascal.interperter.ast.variablecontext.VariableContext;
import com.duy.pascal.interperter.debugable.DebuggableExecutableReturnValue;
import com.duy.pascal.interperter.linenumber.LineInfo;
import com.duy.pascal.interperter.parse_exception.ParsingException;
import com.duy.pascal.interperter.parse_exception.define.AmbiguousFunctionCallException;
import com.duy.pascal.interperter.parse_exception.define.BadFunctionCallException;
import com.duy.pascal.interperter.runtime_exception.RuntimePascalException;
import com.duy.pascal.interperter.tokens.WordToken;
import com.duy.pascal.interperter.utils.ArrayUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class FunctionCall extends DebuggableExecutableReturnValue {
    protected static final String TAG = FunctionCall.class.getSimpleName();
    public RuntimeValue[] arguments;
    private LineInfo lineInfo;

    public static FunctionCall generateFunctionCall(WordToken name, List<RuntimeValue> arguments,
                                                    ExpressionContext expressionContext)
            throws ParsingException {
        List<List<AbstractFunction>> possibilities = new ArrayList<>();
        expressionContext.getCallableFunctions(name.name.toLowerCase(), possibilities);

        boolean matching = false;
        boolean perfectFit = false;

        AbstractFunction chosen = null;
        AbstractFunction ambiguous = null;
        FunctionCall result;
        FunctionCall runtimeValue = null;

        for (List<AbstractFunction> l : possibilities) {
            for (AbstractFunction function : l) {
                result = function.generatePerfectFitCall(name.getLineNumber(), arguments, expressionContext);
                if (result != null) {
                    if (perfectFit) {
                        throw new AmbiguousFunctionCallException(name.getLineNumber(), chosen, function);
                    }
                    perfectFit = true;
                    chosen = function;
                    runtimeValue = result;
                    break;
                }
                result = function.generateCall(name.getLineNumber(), arguments, expressionContext);
                if (result != null && !perfectFit) {
                    if (chosen != null) {
                        ambiguous = chosen;
                    }
                    chosen = function;
                    if (runtimeValue == null)
                        runtimeValue = result;
                }
                if (function.argumentTypes().length == arguments.size()) {
                    matching = true;
                }
            }
        }
        if (runtimeValue == null) {
            ArrayList<String> argsType = new ArrayList<>();
            for (int i = 0; i < arguments.size(); i++) {
                argsType.add(String.valueOf(arguments.get(i).getRuntimeType(expressionContext)));
            }
            ArrayList<String> listFunctions = new ArrayList<>();
            for (List<AbstractFunction> possibility : possibilities) {
                for (AbstractFunction function : possibility) {
                    listFunctions.add(function.toString());
                }
            }
            throw new BadFunctionCallException(name.getLineNumber(),
                    name.name, !possibilities.isEmpty(), matching, argsType, listFunctions, expressionContext);
        } else if (!perfectFit && ambiguous != null) {
            throw new AmbiguousFunctionCallException(name.getLineNumber(), chosen, ambiguous);
        } else {
            return runtimeValue;
        }
    }

    @Override
    public String toString() {
        return getFunctionName() + ArrayUtil.argToString(arguments);
    }

    protected abstract String getFunctionName();

    @Override
    public ExecutionResult executeImpl(VariableContext f,
                                       RuntimeExecutableCodeUnit<?> main)
            throws RuntimePascalException {
        Object valueImpl = getValueImpl(f, main);
        if (valueImpl == ExecutionResult.EXIT) {
            return ExecutionResult.EXIT;
        }
        return ExecutionResult.NOPE;
    }

    @Override
    public Object compileTimeValue(CompileTimeContext context)
            throws ParsingException {
        return null;
    }

    @NonNull
    @Override
    public LineInfo getLineNumber() {
        return lineInfo;
    }

    @Override
    public void setLineNumber(LineInfo lineNumber) {
        this.lineInfo = lineNumber;
    }

    protected RuntimeValue[] compileTimeExpressionFoldArguments(CompileTimeContext context)
            throws ParsingException {
        RuntimeValue[] args = new RuntimeValue[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            args[i] = arguments[i].compileTimeExpressionFold(context);
        }
        return args;
    }
}