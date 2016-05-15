package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class Begin {
    public static Expr create(Expr... es) {
        return new BeginImpl(es);
    }

    static class BeginImpl extends Expr {
        @Children
        protected final Expr[] es;

        BeginImpl(Expr[] es) {
            this.es = es;
        }

        @Override
        @ExplodeLoop
        public Object executeGeneric(VirtualFrame frame) {
            CompilerAsserts.compilationConstant(es.length);

            Object res = Symbol.apply("#void");
            for (Expr e : es)  {
                res = e.executeGeneric(frame);
            }
            return res;
        }
    }
}
