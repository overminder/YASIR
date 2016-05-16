package com.github.overmind.yasir.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class Lit {
    public static Expr create(long v) {
        return new LitLong(v);
    }

    public static Expr create(Object o) {
        return new LitObject(o);
    }

    private static class LitLong extends Expr {
        private final long v;

        public LitLong(long v) {
            super();
            this.v = v;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return v;
        }
    }

    private static class LitObject extends Expr {
        private final Object o;

        public LitObject(Object o) {
            super();
            this.o = o;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return o;
        }
    }
}
