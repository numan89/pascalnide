package com.duy.pascal.backend.tokens;

import com.duy.pascal.backend.linenumber.LineInfo;

/**
 * Comment token
 * Created by Duy on 21-Mar-17.
 */
public class CommentToken extends Token {
    public String comment;


    public CommentToken(LineInfo line, String cmt) {
        super(line);
        this.comment = cmt;
    }

    @Override
    public String toString() {
        if (comment.endsWith("\n"))
            return comment;
        else {
            return comment + "\n";
        }
    }
}