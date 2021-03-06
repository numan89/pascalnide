package com.duy.pascal.interperter.runtime_exception;

import com.duy.pascal.interperter.declaration.lang.function.AbstractFunction;
import com.duy.pascal.interperter.linenumber.LineInfo;

public class MethodCallException extends RuntimePascalException {
    public Throwable cause;
    public AbstractFunction function;

    public MethodCallException(LineInfo line, Throwable cause,
                               AbstractFunction function) {
        super(line);
        this.cause = cause;
        this.function = function;
    }

    @Override
    public String getMessage() {
        return "When calling Function or Procedure " + function.getName()
                + ", The following java exception: \"" + cause + "\"\n" +
                "Message: " + (cause != null ? cause.getMessage() : "");
    }
}
