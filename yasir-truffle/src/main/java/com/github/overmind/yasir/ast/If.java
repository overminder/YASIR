package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.interp.InterpException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

public final class If {
    public static Expr create(Expr c0, Expr t0, Expr f0) {
        return new Expr() {
            @Child protected Expr c = c0;
            @Child protected Expr t = t0;
            @Child protected Expr f = f0;

            private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

            @Override
            public Object executeGeneric(VirtualFrame frame) {
                try {
                    if (profile.profile(c.executeBoolean(frame))) {
                        return t.executeGeneric(frame);
                    } else {
                        return f.executeGeneric(frame);
                    }
                } catch (UnexpectedResultException e) {
                    throw InterpException.unexpected(e);
                }
            }
        };
    }
}
