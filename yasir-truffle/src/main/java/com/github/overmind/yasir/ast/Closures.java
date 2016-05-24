package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.value.BareFunction;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class Closures {
    public static Expr alloc(BareFunction base, Expr... payloads) {
        return new MkClosure(base, payloads);
    }

    static class MkClosure extends Expr {
        private final BareFunction base;

        @Children private final Expr[] payloads;

        MkClosure(BareFunction base, Expr[] payloads) {
            this.base = base;
            this.payloads = payloads;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            Object[] payloads = ApplyNode.evalArgs(frame, this.payloads);
            return base.withPayloads(payloads);
        }
    }
}
