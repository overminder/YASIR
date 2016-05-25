package com.github.overmind.yasir.interp;

import com.github.overmind.yasir.value.BareFunction;
import com.oracle.truffle.api.nodes.ControlFlowException;

public final class InterpException {
    public static TrampolineException tailCall(BareFunction func, Object[] args) {
        return new TrampolineException(func, args);
    }

    public final static class TrampolineException extends ControlFlowException {
        public final BareFunction func;
        public final Object[] args;

        public TrampolineException(BareFunction target, Object[] args) {
            this.func = target;
            this.args = args;
        }
    }
}
