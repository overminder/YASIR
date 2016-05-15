package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.interp.InterpException;
import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Callable;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;

final public class PrimOps {
    public static Expr add(Expr lhs, Expr rhs) {
        return createBinary(ADD_VALUE, lhs, rhs);
    }

    public static Expr sub(Expr lhs, Expr rhs) {
        return createBinary(SUB_VALUE, lhs, rhs);
    }

    public static Expr lessThan(Expr lhs, Expr rhs) {
        return createBinary(LT_VALUE, lhs, rhs);
    }

    public static Expr createBinary(Callable op, Expr lhs, Expr rhs) {
        return Apply.create(Const.create(op), lhs, rhs);
    }

    static class PrimOpValue extends Callable {
        private final CallTarget target;
        public final PrimBinaryArithOp expr;

        protected PrimOpValue(PrimBinaryArithOp expr) {
            this.expr = expr;
            target = Yasir.rt().createCallTarget(expr);
        }

        @Override
        public final Object payload() {
            return null;
        }

        @Override
        public CallTarget target() {
            return target;
        }
    }

    static class PrimBinaryArithOp extends FramelessExpr {
        public final BinaryArithOpImpl opImpl;

        protected PrimBinaryArithOp(BinaryArithOpImpl opImpl) {
            this.opImpl = opImpl;
        }

        @Override
        public final Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            if (args.length != 3) {
                throw InterpException.unexpected("Wrong argc");
            }
            long lhs = (Long) args[1];
            long rhs = (Long) args[2];
            return opImpl.doLong(lhs, rhs);
        }
    }

    interface BinaryArithOpImpl {
        Object doLong(long lhs, long rhs);
    }

    protected static PrimBinaryArithOp mkOpExpr(BinaryArithOpImpl opImpl) {
        return new PrimBinaryArithOp(opImpl);
    }

    static final PrimBinaryArithOp ADD_OP = mkOpExpr((lhs, rhs) -> lhs + rhs);
    static final PrimBinaryArithOp SUB_OP = mkOpExpr((lhs, rhs) -> lhs - rhs);
    static final PrimBinaryArithOp LT_OP = mkOpExpr((lhs, rhs) -> lhs < rhs);

    static final PrimOpValue ADD_VALUE = new PrimOpValue(ADD_OP);
    static final PrimOpValue SUB_VALUE = new PrimOpValue(SUB_OP);
    static final PrimOpValue LT_VALUE = new PrimOpValue(LT_OP);
}
