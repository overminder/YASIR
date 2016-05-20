package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.interp.InterpException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

public final class If {
    public static Expr create(Expr c, Expr t, Expr f) {
        return new IfImpl(c, t, f);
    }

    static final class IfImpl extends Expr {
        @Child protected Expr c;
        @Child protected Expr t;
        @Child protected Expr f;


        private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

        public IfImpl(Expr c, Expr t, Expr f) {
            this.c = c;
            this.t = t;
            this.f = f;
        }

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
