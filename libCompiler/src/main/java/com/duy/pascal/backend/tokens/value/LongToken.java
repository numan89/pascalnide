package com.duy.pascal.backend.tokens.value;


import com.duy.pascal.backend.linenumber.LineInfo;

public class LongToken extends ValueToken {
    private Long cacheValue = null;
    private String value;

    public LongToken(LineInfo line, String value) {
        super(line);
        this.value = value;
        if (this.line != null) {
            this.line.setLength(value.length());
        }
    }

    @Override
    public Object getValue() {
        if (cacheValue != null) return cacheValue;
        cacheValue = Long.parseLong(value);
        return cacheValue;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String toCode() {
        return value;
    }
}
