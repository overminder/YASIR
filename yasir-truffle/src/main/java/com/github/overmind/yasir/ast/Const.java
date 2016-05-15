package com.github.overmind.yasir.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class Const {
    public static Expr create(long v) {
        return new ConstLong(v);
    }

    public static Expr create(Object o) {
        return new ConstObject(o);
    }

    private static class ConstLong extends Expr {
        private final long v;

        public ConstLong(long v) {
            super();
            this.v = v;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return v;
        }
    }

    private static class ConstObject extends Expr {
        private final Object o;

        public ConstObject(Object o) {
            super();
            this.o = o;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return o;
        }
    }
}
