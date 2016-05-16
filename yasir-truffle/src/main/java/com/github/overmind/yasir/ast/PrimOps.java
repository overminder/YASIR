package com.github.overmind.yasir.ast;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

final public class PrimOps {
    public static Expr add(Expr lhs, Expr rhs) {
        return PrimOpsFactory.AddNodeGen.create(lhs, rhs);
    }

    public static Expr sub(Expr lhs, Expr rhs) {
        return PrimOpsFactory.SubNodeGen.create(lhs, rhs);
    }

    public static Expr lessThan(Expr lhs, Expr rhs) {
        return PrimOpsFactory.LessThanNodeGen.create(lhs, rhs);
    }

    @NodeInfo(shortName = "+")
    abstract static class Add extends Binary {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected long add(long lhs, long rhs) {
            return lhs + rhs;
        }
    }

    @NodeInfo(shortName = "-")
    abstract static class Sub extends Binary {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected long sub(long lhs, long rhs) {
            return lhs - rhs;
        }
    }

    @NodeInfo(shortName = "<")
    abstract static class LessThan extends Binary {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected boolean lessThan(long lhs, long rhs) {
            return lhs < rhs;
        }
    }
}
