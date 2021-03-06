package com.duy.pascal.interperter.ast.instructions.conditional;

import com.duy.pascal.interperter.ast.codeunit.RuntimeExecutableCodeUnit;
import com.duy.pascal.interperter.ast.expressioncontext.CompileTimeContext;
import com.duy.pascal.interperter.ast.expressioncontext.ExpressionContext;
import com.duy.pascal.interperter.ast.instructions.Executable;
import com.duy.pascal.interperter.ast.instructions.ExecutionResult;
import com.duy.pascal.interperter.ast.variablecontext.VariableContext;
import com.duy.pascal.interperter.ast.runtime_value.value.RuntimeValue;
import com.duy.pascal.interperter.debugable.DebuggableExecutable;
import com.duy.pascal.interperter.linenumber.LineInfo;
import com.duy.pascal.interperter.parse_exception.ParsingException;
import com.duy.pascal.interperter.parse_exception.convert.UnConvertibleTypeException;
import com.duy.pascal.interperter.parse_exception.syntax.ExpectThenTokenException;
import com.duy.pascal.interperter.parse_exception.syntax.ExpectedTokenException;
import com.duy.pascal.interperter.runtime_exception.RuntimePascalException;
import com.duy.pascal.interperter.tokens.Token;
import com.duy.pascal.interperter.tokens.basic.BasicToken;
import com.duy.pascal.interperter.tokens.basic.ElseToken;
import com.duy.pascal.interperter.tokens.basic.ThenToken;
import com.duy.pascal.interperter.tokens.grouping.GrouperToken;
import com.duy.pascal.interperter.declaration.lang.types.BasicType;

public class IfStatement extends DebuggableExecutable {
    private RuntimeValue condition;
    private Executable instruction;
    private Executable elseInstruction;
    private LineInfo line;

    public IfStatement(RuntimeValue condition, Executable instruction,
                       Executable elseInstruction, LineInfo line) {


        this.condition = condition;
        this.instruction = instruction;
        this.elseInstruction = elseInstruction;
        this.line = line;
    }

    /**
     * Declaration if statement
     * <p>
     * if <condition> then <command>
     * <p>
     * if <condition> then <command> else <commnad>
     *
     * @param lineNumber - the lineInfo of begin if token
     */
    public IfStatement(ExpressionContext context, GrouperToken grouperToken, LineInfo lineNumber) throws ParsingException {

        //check condition is boolean value
        RuntimeValue condition = grouperToken.getNextExpression(context);
        RuntimeValue convert = BasicType.Boolean.convert(condition, context);
        if (convert == null) {
            throw new UnConvertibleTypeException(condition, BasicType.Boolean,
                    condition.getRuntimeType(context).declType, context);
        }

        //check then token
        Token next = grouperToken.take();
        if (!(next instanceof ThenToken)) {
            if (next instanceof BasicToken) {
                throw new ExpectedTokenException("then", next);
            } else {
                throw new ExpectThenTokenException(next.getLineNumber());
            }
        }

        //get command after then token
        Executable command = grouperToken.getNextCommand(context);

        //if it in include else command
        Executable elseCommand = null;
        next = grouperToken.peek();
        if (next instanceof ElseToken) {
            grouperToken.take();
            elseCommand = grouperToken.getNextCommand(context);
        }

        this.condition = condition;
        this.instruction = command;
        this.elseInstruction = elseCommand;
        this.line = lineNumber;
    }

    @Override
    public LineInfo getLineNumber() {
        return line;
    }

    @Override
    public ExecutionResult executeImpl(VariableContext context,
                                       RuntimeExecutableCodeUnit<?> main) throws RuntimePascalException {
        Boolean value = (Boolean) (condition.getValue(context, main));
        if (value) {
            return instruction.execute(context, main);
        } else {
            if (elseInstruction != null) {
                return elseInstruction.execute(context, main);
            }
            return ExecutionResult.NOPE;
        }
    }

    @Override
    public String toString() {
        return "if [" + condition.toString() + "] then [\n" + instruction + ']';
    }

    @Override
    public Executable compileTimeConstantTransform(CompileTimeContext c)
            throws ParsingException {
        Object o = condition.compileTimeValue(c);
        if (o != null) {
            Boolean b = (Boolean) o;
            if (b) {
                return instruction.compileTimeConstantTransform(c);
            } else {
                return elseInstruction.compileTimeConstantTransform(c);
            }
        } else {
            return new IfStatement(condition,
                    instruction.compileTimeConstantTransform(c),
                    elseInstruction.compileTimeConstantTransform(c), line);
        }
    }
}
