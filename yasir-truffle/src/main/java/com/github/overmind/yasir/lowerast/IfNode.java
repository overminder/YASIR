package com.github.overmind.yasir.lowerast;

import com.github.overmind.yasir.ast.Expr;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

public final class IfNode extends Expr {
    @Child
    private Expr cond;
    @Child
    private Expr onTrue;
    @Child
    private Expr onFalse;

    private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

    public IfNode(Expr cond, Expr onTrue, Expr onFalse) {
        this.cond = cond;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            // Binary profiles are not good for things like fibonacci: predicting the
            // branch is useless in this case.
            // However it will be pretty useful in other cases...
            if (profile.profile(cond.executeBoolean(frame))) {
                return onTrue.executeGeneric(frame);
            } else {
                return onFalse.executeGeneric(frame);
            }
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }
}
