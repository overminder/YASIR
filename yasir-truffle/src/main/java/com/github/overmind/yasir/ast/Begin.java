package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class Begin {
    public static Expr create(Expr... es) {
        return new Simple(es);
    }

    static class Simple extends FramelessExpr {
        @Children
        protected final Expr[] es;

        Simple(Expr[] es) {
            this.es = es;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            Object res = Symbol.apply("#void");
            for (Expr e : es)  {
                res = e.execute(frame);
            }
            return res;
        }
    }
}
