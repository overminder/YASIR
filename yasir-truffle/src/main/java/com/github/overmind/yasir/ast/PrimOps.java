package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.interp.InterpException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

final public class PrimOps {
    public static Expr add(Expr lhs, Expr rhs) {
        return createBinary((x, y) -> x + y, lhs, rhs);
    }

    public static Expr sub(Expr lhs, Expr rhs) {
        return createBinary((x, y) -> x - y, lhs, rhs);
    }

    public static Expr lessThan(Expr lhs, Expr rhs) {
        return createBinary((x, y) -> x < y, lhs, rhs);
    }

    protected static Expr createBinary(BinaryArithOpImpl opImpl, Expr lhs, Expr rhs) {
        return new BinaryLongExpr(lhs, rhs) {
            @Override
            Object doLong(long lhs, long rhs) {
                return opImpl.doLong(lhs, rhs);
            }
        };
    }

    static abstract class BinaryLongExpr extends Expr {
        @Child
        protected Expr lhs;

        @Child
        protected Expr rhs;

        BinaryLongExpr(Expr lhs, Expr rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        abstract Object doLong(long lhs, long rhs);

        @Override
        Object executeGeneric(VirtualFrame frame) {
            try {
                return doLong(lhs.executeLong(frame), rhs.executeLong(frame));
            } catch (UnexpectedResultException e) {
                throw InterpException.unexpected(e);
            }
        }
    }

    interface BinaryArithOpImpl {
        Object doLong(long lhs, long rhs);
    }
}
