package com.duy.pascal.backend.runtime.operators.number;


import com.duy.pascal.backend.exceptions.operator.DivisionByZeroException;
import com.duy.pascal.backend.exceptions.ParsingException;
import com.duy.pascal.backend.linenumber.LineInfo;
import com.duy.pascal.backend.pascaltypes.BasicType;
import com.duy.pascal.backend.pascaltypes.RuntimeType;
import com.duy.pascal.backend.pascaltypes.OperatorTypes;
import com.duy.pascal.backend.runtime.operators.BinaryOperatorEval;
import com.js.interpreter.expressioncontext.CompileTimeContext;
import com.js.interpreter.expressioncontext.ExpressionContext;
import com.duy.pascal.backend.runtime.value.ConstantAccess;
import com.duy.pascal.backend.runtime.value.RuntimeValue;
import com.duy.pascal.backend.runtime.exception.PascalArithmeticException;
import com.duy.pascal.backend.runtime.exception.internal.InternalInterpreterException;

public class DoubleBiOperatorEval extends BinaryOperatorEval {

    public DoubleBiOperatorEval(RuntimeValue operon1, RuntimeValue operon2,
                                OperatorTypes operator, LineInfo line) {
        super(operon1, operon2, operator, line);
    }

    @Override
    public RuntimeType getType(ExpressionContext f) throws ParsingException {
        switch (operator_type) {
            case EQUALS:
            case GREATEREQ:
            case GREATERTHAN:
            case LESSEQ:
            case LESSTHAN:
            case NOTEQUAL:
                return new RuntimeType(BasicType.Boolean, false);
            default:
                return new RuntimeType(BasicType.Double, false);
        }
    }

    @Override
    public Object operate(Object value1, Object value2)
            throws PascalArithmeticException, InternalInterpreterException {
        double v1 = Double.valueOf(String.valueOf(value1));
        double v2 = Double.valueOf(String.valueOf(value2));
        switch (operator_type) {
            case DIVIDE:
                if (Math.abs(v2) == 0d) {
                    throw new DivisionByZeroException(line);
                }
                return v1 / v2;
            case EQUALS:
                return v1 == v2;
            case GREATEREQ:
                return v1 >= v2;
            case GREATERTHAN:
                return v1 > v2;
            case LESSEQ:
                return v1 <= v2;
            case LESSTHAN:
                return v1 < v2;
            case MINUS:
                return v1 - v2;
            case MULTIPLY:
                return v1 * v2;
            case NOTEQUAL:
                return v1 != v2;
            case PLUS:
                return v1 + v2;
            default:
                throw new InternalInterpreterException(line);
        }
    }

    @Override
    public RuntimeValue compileTimeExpressionFold(CompileTimeContext context) throws ParsingException {
        Object val = this.compileTimeValue(context);
        if (val != null) {
            return new ConstantAccess(val, line);
        } else {
            return new DoubleBiOperatorEval(
                    operon1.compileTimeExpressionFold(context),
                    operon2.compileTimeExpressionFold(context), operator_type,
                    line);
        }
    }
}