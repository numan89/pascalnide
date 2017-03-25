package com.duy.pascal.backend.tokens.grouping;


import com.duy.pascal.backend.exceptions.BadOperationTypeException;
import com.duy.pascal.backend.exceptions.ExpectedAnotherTokenException;
import com.duy.pascal.backend.exceptions.ExpectedTokenException;
import com.duy.pascal.backend.exceptions.MultipleDefaultValuesException;
import com.duy.pascal.backend.exceptions.NonConstantExpressionException;
import com.duy.pascal.backend.exceptions.NonIntegerIndexException;
import com.duy.pascal.backend.exceptions.NotAStatementException;
import com.duy.pascal.backend.exceptions.ParsingException;
import com.duy.pascal.backend.exceptions.UnconvertibleTypeException;
import com.duy.pascal.backend.exceptions.UnrecognizedTokenException;
import com.duy.pascal.backend.exceptions.grouping.GroupingException;
import com.duy.pascal.backend.linenumber.LineInfo;
import com.duy.pascal.backend.pascaltypes.ArrayType;
import com.duy.pascal.backend.pascaltypes.BasicType;
import com.duy.pascal.backend.pascaltypes.DeclaredType;
import com.duy.pascal.backend.pascaltypes.RecordType;
import com.duy.pascal.backend.pascaltypes.RuntimeType;
import com.duy.pascal.backend.pascaltypes.SubrangeType;
import com.duy.pascal.backend.tokens.CommentToken;
import com.duy.pascal.backend.tokens.EOF_Token;
import com.duy.pascal.backend.tokens.GroupingExceptionToken;
import com.duy.pascal.backend.tokens.OperatorToken;
import com.duy.pascal.backend.tokens.OperatorTypes;
import com.duy.pascal.backend.tokens.Token;
import com.duy.pascal.backend.tokens.WordToken;
import com.duy.pascal.backend.tokens.basic.ArrayToken;
import com.duy.pascal.backend.tokens.basic.AssignmentToken;
import com.duy.pascal.backend.tokens.basic.BreakToken;
import com.duy.pascal.backend.tokens.basic.ColonToken;
import com.duy.pascal.backend.tokens.basic.CommaToken;
import com.duy.pascal.backend.tokens.basic.DoToken;
import com.duy.pascal.backend.tokens.basic.DowntoToken;
import com.duy.pascal.backend.tokens.basic.ElseToken;
import com.duy.pascal.backend.tokens.basic.ExitToken;
import com.duy.pascal.backend.tokens.basic.ForToken;
import com.duy.pascal.backend.tokens.basic.IfToken;
import com.duy.pascal.backend.tokens.basic.OfToken;
import com.duy.pascal.backend.tokens.basic.PeriodToken;
import com.duy.pascal.backend.tokens.basic.RepeatToken;
import com.duy.pascal.backend.tokens.basic.SemicolonToken;
import com.duy.pascal.backend.tokens.basic.ThenToken;
import com.duy.pascal.backend.tokens.basic.ToToken;
import com.duy.pascal.backend.tokens.basic.UntilToken;
import com.duy.pascal.backend.tokens.basic.WhileToken;
import com.duy.pascal.backend.tokens.value.ValueToken;
import com.js.interpreter.ast.VariableDeclaration;
import com.js.interpreter.ast.expressioncontext.ExpressionContext;
import com.js.interpreter.ast.instructions.BreakInstruction;
import com.js.interpreter.ast.instructions.Executable;
import com.js.interpreter.ast.instructions.InstructionGrouper;
import com.js.interpreter.ast.instructions.NopInstruction;
import com.js.interpreter.ast.instructions.ReturnInstruction;
import com.js.interpreter.ast.instructions.case_statement.CaseInstruction;
import com.js.interpreter.ast.instructions.conditional.DowntoForStatement;
import com.js.interpreter.ast.instructions.conditional.ForStatement;
import com.js.interpreter.ast.instructions.conditional.IfStatement;
import com.js.interpreter.ast.instructions.conditional.RepeatInstruction;
import com.js.interpreter.ast.instructions.conditional.WhileStatement;
import com.js.interpreter.ast.returnsvalue.ConstantAccess;
import com.js.interpreter.ast.returnsvalue.FieldAccess;
import com.js.interpreter.ast.returnsvalue.FunctionCall;
import com.js.interpreter.ast.returnsvalue.ReturnsValue;
import com.js.interpreter.ast.returnsvalue.UnaryOperatorEvaluation;
import com.js.interpreter.ast.returnsvalue.operators.BinaryOperatorEvaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class GrouperToken extends Token {
    LinkedBlockingQueue<Token> queue;
    Token next = null;

    public GrouperToken(LineInfo line) {
        super(line);
        queue = new LinkedBlockingQueue<Token>();
    }

    private Token get_next() throws GroupingException {
        if (next == null) {
            while (true) {
                try {
                    next = queue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                break;
            }
        }
        exceptionCheck(next);
        return next;
    }

    public boolean hasNext() throws GroupingException {
        return !(get_next() instanceof EOF_Token);
    }

    private void exceptionCheck(Token t) throws GroupingException {
        if (t instanceof GroupingExceptionToken) {
            throw ((GroupingExceptionToken) t).exception;
        }
    }

    public void put(Token t) {
        while (true) {
            try {
                queue.put(t);
            } catch (InterruptedException e) {
                continue;
            }
            break;
        }
    }

    public abstract String toCode();

    public Token take() throws ExpectedAnotherTokenException, GroupingException {
        Token result = get_next();
        if (result instanceof EOF_Token) {
            throw new ExpectedAnotherTokenException(result.lineInfo);
        }
        while (true) {
            try {
                next = queue.take();
                exceptionCheck(next);
                return result;
            } catch (InterruptedException ignored) {
            }
        }
    }

    public Token peek() throws GroupingException {
        return get_next();
    }

    public Token peek_no_EOF() throws ExpectedAnotherTokenException, GroupingException {
        Token result = peek();
        if (result instanceof EOF_Token) {
            throw new ExpectedAnotherTokenException(result.lineInfo);
        }
        return result;
    }

    @Override
    public String toString() {
        try {
            return get_next().toString() + ',' + queue.toString();
        } catch (GroupingException e) {
            return "Exception: " + e.toString();
        }
    }

    public String next_word_value() throws ParsingException {
        return take().get_word_value().name;
    }

    public void assertNextSemicolon() throws ParsingException {
        Token t = take();
        if (!(t instanceof SemicolonToken)) {
            throw new ExpectedTokenException(";", t);
        }
    }

    public void assert_next_comma() throws ParsingException {
        Token t = take();
        if (!(t instanceof CommaToken)) {
            throw new ExpectedTokenException(",", t);
        }
    }

    public DeclaredType getNextPascalType(ExpressionContext context) throws ParsingException {
        Token n = take();
        if (n instanceof ArrayToken) {
            return getArrayType(context);
        }
        if (n instanceof RecordToken) {
            RecordToken r = (RecordToken) n;
            RecordType result = new RecordType();
            result.variable_types = r.getVariableDeclarations(context);
            return result;
        }
        if (!(n instanceof WordToken)) {
            throw new ExpectedTokenException("[Type Identifier]", n);
        }
        return ((WordToken) n).to_basic_type(context);
    }

    private DeclaredType getArrayType(ExpressionContext context) throws ParsingException {
        Token n = peek_no_EOF();
        if (n instanceof BracketedToken) {
            BracketedToken bracket = (BracketedToken) take();
            return getArrayType(bracket, context);
        } else if (n instanceof OfToken) {
            take();
            DeclaredType elementType = getNextPascalType(context);
            ArrayType<DeclaredType> declaredTypeArrayType = new ArrayType<>(elementType, new SubrangeType());
            return declaredTypeArrayType;
        } else {
            throw new ExpectedTokenException("of", n);
        }

    }

    private DeclaredType getArrayType(BracketedToken bounds, ExpressionContext context)
            throws ParsingException {
        SubrangeType bound = new SubrangeType(bounds, context);
        DeclaredType elementType;
        if (bounds.hasNext()) {
            Token t = bounds.take();
            if (!(t instanceof CommaToken)) {
                throw new ExpectedTokenException("']' or ','", t);
            }
            elementType = getArrayType(bounds, context);
        } else {
            Token next = take();
            if (!(next instanceof OfToken)) {
                throw new ExpectedTokenException("of", next);
            }
            elementType = getNextPascalType(context);
        }
        return new ArrayType<>(elementType, bound);
    }

    public ReturnsValue getNextExpression(ExpressionContext context,
                                          precedence precedence, Token next) throws ParsingException {
        ReturnsValue nextTerm;
        if (next instanceof OperatorToken) {
            OperatorToken nextOperator = (OperatorToken) next;
            if (!nextOperator.can_be_unary()) {
                throw new BadOperationTypeException(next.lineInfo,
                        nextOperator.type);
            }
            nextTerm = new UnaryOperatorEvaluation(getNextExpression(context,
                    nextOperator.type.getPrecedence()), nextOperator.type,
                    nextOperator.lineInfo);
        } else {
            nextTerm = getNextTerm(context, next);
        }
        while ((next = peek()).getOperatorPrecedence() != null) {
            if (next instanceof OperatorToken) {
                OperatorToken nextOperator = (OperatorToken) next;
                if (nextOperator.type.getPrecedence().compareTo(precedence) >= 0) {
                    break;
                }
                take();
                ReturnsValue nextvalue = getNextExpression(context,
                        nextOperator.type.getPrecedence());
                OperatorTypes operationtype = ((OperatorToken) next).type;
                DeclaredType type1 = nextTerm.getType(context).declType;
                DeclaredType type2 = nextvalue.getType(context).declType;
                try {
                    operationtype.verifyOperation(type1, type2);
                } catch (BadOperationTypeException e) {
                    throw new BadOperationTypeException(next.lineInfo, type1,
                            type2, nextTerm, nextvalue, operationtype);
                }
                nextTerm = BinaryOperatorEvaluation.generateOp(context,
                        nextTerm, nextvalue, operationtype,
                        nextOperator.lineInfo);
            } else if (next instanceof PeriodToken) {
                take();
                next = take();
                if (!(next instanceof WordToken)) {
                    throw new ExpectedTokenException("[Element Name]", next);
                }
                nextTerm = new FieldAccess(nextTerm, (WordToken) next);
            } else if (next instanceof BracketedToken) {
                take();
                BracketedToken b = (BracketedToken) next;
                RuntimeType t = nextTerm.getType(context);
                ReturnsValue v = b.getNextExpression(context);
                ReturnsValue converted = BasicType.Integer.convert(v, context);
                if (converted == null) {
                    throw new NonIntegerIndexException(v);
                }
                if (b.hasNext()) {
                    throw new ExpectedTokenException("]", b.take());
                }
                nextTerm = t.declType.generateArrayAccess(nextTerm, converted);
            }
        }
        return nextTerm;
    }

    public ReturnsValue getNextExpression(ExpressionContext context,
                                          precedence precedence) throws ParsingException {
        return getNextExpression(context, precedence, take());
    }

    public ReturnsValue getNextTerm(ExpressionContext context, Token next)
            throws ParsingException {
        if (next instanceof ParenthesizedToken) {
            return ((ParenthesizedToken) next).getSingleValue(context);
        } else if (next instanceof ValueToken) {
            return new ConstantAccess(((ValueToken) next).getValue(),
                    next.lineInfo);
        } else if (next instanceof WordToken) {
            WordToken name = ((WordToken) next);
            next = peek();

            if (next instanceof ParenthesizedToken) {
                List<ReturnsValue> arguments = ((ParenthesizedToken) take())
                        .get_arguments_for_call(context);
                return FunctionCall.generateFunctionCall(name, arguments,
                        context);
            } else {
                return context.getIdentifierValue(name);
            }
        } else if (next instanceof CommentToken) {
            // TODO: 21-Mar-17 ignore comment token
            take();
            return getNextTerm(context);
        } else {
            throw new UnrecognizedTokenException(next);
        }
    }

    public ReturnsValue getNextTerm(ExpressionContext context)
            throws ParsingException {
        return getNextTerm(context, take());
    }

    public ReturnsValue getNextExpression(ExpressionContext context)
            throws ParsingException {
        return getNextExpression(context, precedence.NoPrecedence);
    }

    public ReturnsValue getNextExpression(ExpressionContext context, Token first)
            throws ParsingException {
        return getNextExpression(context, precedence.NoPrecedence, first);
    }

    public List<VariableDeclaration> getVariableDeclarations(
            ExpressionContext context) throws ParsingException {
        List<VariableDeclaration> result = new ArrayList<VariableDeclaration>();
        /*
         * reusing it, so it is further out of scope than necessary
		 */
        List<WordToken> names = new ArrayList<WordToken>();
        Token next;
        do {
            do {
                next = take();
                if (!(next instanceof WordToken)) {
                    throw new ExpectedTokenException("[Variable Identifier]",
                            next);
                }
                names.add((WordToken) next);
                next = take();
            } while (next instanceof CommaToken);
            if (!(next instanceof ColonToken)) {
                throw new ExpectedTokenException(":", next);
            }
            DeclaredType type;
            type = getNextPascalType(context);

            Object defaultValue = null;
            if (peek() instanceof OperatorToken) {
                if (((OperatorToken) peek()).type == OperatorTypes.EQUALS) {
                    take();
                    ReturnsValue unconverted = getNextExpression(context);
                    ReturnsValue converted = type.convert(unconverted, context);
                    if (converted == null) {
                        throw new UnconvertibleTypeException(unconverted,
                                unconverted.getType(context).declType, type,
                                true);
                    }
                    defaultValue = converted.compileTimeValue(context);
                    if (defaultValue == null) {
                        throw new NonConstantExpressionException(converted);
                    }
                    if (names.size() != 1) {
                        throw new MultipleDefaultValuesException(converted.getLineNumber());
                    }
                }
            }
            assertNextSemicolon();
            for (WordToken s : names) {
                VariableDeclaration v = new VariableDeclaration(s.name, type,
                        defaultValue, s.lineInfo);
                context.verifyNonConflictingSymbol(v);
                result.add(v);
            }
            names.clear(); // reusing the list object
            next = peek();
        } while (next instanceof WordToken);
        return result;
    }

    public ReturnsValue getSingleValue(ExpressionContext context)
            throws ParsingException {
        ReturnsValue result = getNextExpression(context);
        if (hasNext()) {
            Token next = take();
            throw new ExpectedTokenException(getClosingText(), next);
        }
        return result;
    }

    public Executable getNextCommand(ExpressionContext context) throws ParsingException {
        Token next = take();
        LineInfo initialline = next.lineInfo;
        if (next instanceof IfToken) {
            ReturnsValue condition = getNextExpression(context);
            next = take();
            assert (next instanceof ThenToken);
            Executable command = getNextCommand(context);
            Executable else_command = null;
            next = peek();
            if (next instanceof ElseToken) {
                take();
                else_command = getNextCommand(context);
            }
            return new IfStatement(condition, command, else_command, initialline);
        } else if (next instanceof WhileToken) {
            ReturnsValue condition = getNextExpression(context);
            next = take();
            assert (next instanceof DoToken);
            Executable command = getNextCommand(context);
            return new WhileStatement(condition, command, initialline);
        } else if (next instanceof BeginEndToken) {
            InstructionGrouper beginEndPreprocessed = new InstructionGrouper(initialline);
            BeginEndToken castToken = (BeginEndToken) next;
            while (castToken.hasNext()) {
                beginEndPreprocessed.addCommand(castToken.getNextCommand(context));
                if (castToken.hasNext()) {
                    castToken.assertNextSemicolon();
                }
            }
            return beginEndPreprocessed;
        } else if (next instanceof ForToken) {
            //for i := 1 to n do writeln(i);
            ReturnsValue tmpVar = getNextExpression(context);              //i
            next = take();                                                  // :=
            assert (next instanceof AssignmentToken);
            ReturnsValue firstValue = getNextExpression(context);          //1
            next = take();                                                  //to | downto
            boolean downto = false;
            if (next instanceof DowntoToken) {
                downto = true;
            } else if (!(next instanceof ToToken)) {
                throw new ExpectedTokenException("[To] or [Downto]", next);
            }
            ReturnsValue lastValue = getNextExpression(context);
            next = take();
            assert (next instanceof DoToken);
            Executable result;
            if (downto) { // TODO probably should merge these two types
                result = new DowntoForStatement(context, tmpVar, firstValue,
                        lastValue, getNextCommand(context), initialline);
            } else {
                result = new ForStatement(context, tmpVar, firstValue,
                        lastValue, getNextCommand(context), initialline);
            }
            return result;
        } else if (next instanceof RepeatToken) {
            InstructionGrouper command = new InstructionGrouper(initialline);
            while (!(peek_no_EOF() instanceof UntilToken)) {
                command.addCommand(getNextCommand(context));
                if (!(peek_no_EOF() instanceof UntilToken)) {
                    assertNextSemicolon();
                }
            }
            next = take();
            if (!(next instanceof UntilToken)) {
                throw new ExpectedTokenException("until", next);
            }
            ReturnsValue condition = getNextExpression(context);
            return new RepeatInstruction(command, condition, initialline);
        } else if (next instanceof CaseToken) {
            return new CaseInstruction((CaseToken) next, context);
        } else if (next instanceof SemicolonToken) {
            return new NopInstruction(next.lineInfo);
        } else if (next instanceof BreakToken) {
            return new BreakInstruction(next.lineInfo);
        } else if (next instanceof ExitToken) {
            return new ReturnInstruction(next.lineInfo);
        } else if (next instanceof CommentToken) {
            //ignore comment
            return getNextCommand(context);
        } else {
            try {
                return context.handleUnrecognizedStatement(next, this);
            } catch (ParsingException ignored) {}
            ReturnsValue r = getNextExpression(context, next);
            next = peek();
            if (next instanceof AssignmentToken) {
                take();
                ReturnsValue valueToAssign = getNextExpression(context);
                DeclaredType outputType = r.getType(context).declType;
                DeclaredType inputType = valueToAssign.getType(context).declType;
                /*
                 * Does not have to be writable to assign value to variable.
				 */
                ReturnsValue converted = outputType.convert(valueToAssign, context);
                if (converted == null) {
                    throw new UnconvertibleTypeException(valueToAssign, inputType, outputType, true);
                }
                return r.createSetValueInstruction(outputType.cloneValue(converted));
            } else if (r instanceof Executable) {
                return (Executable) r;
            } else {
                throw new NotAStatementException(r);
            }

        }
    }

    protected abstract String getClosingText();

}
