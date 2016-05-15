package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.interp.InterpException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public final class If {
    public static Expr create(Expr c, Expr t, Expr f) {
        return new FramelessExpr() {
            @Override
            public Object execute(VirtualFrame frame) {
                try {
                    if (c.executeBoolean(frame)) {
                        return t.execute(frame);
                    } else {
                        return f.execute(frame);
                    }
                } catch (UnexpectedResultException e) {
                    throw InterpException.unexpected(e);
                }
            }
        };
    }
}
