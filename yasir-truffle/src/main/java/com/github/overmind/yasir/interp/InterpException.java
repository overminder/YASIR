package com.github.overmind.yasir.interp;

import com.oracle.truffle.api.CallTarget;

public final class InterpException {
    public static RuntimeException unexpected(Exception e) {
        return new UnexpectedInterpException(e);
    }

    public static RuntimeException unexpected(String msg) {
        return new UnexpectedInterpException(new Exception(msg));
    }

    public static TrampolineException tailCall(CallTarget target, Object[] args) {
        return new TrampolineException(target, args);
    }

    static class UnexpectedInterpException extends RuntimeException {
        public final Exception inner;

        UnexpectedInterpException(Exception e) {
            inner = e;
        }
    }

    private static class TrampolineException {
        public final CallTarget target;
        public final Object[] args;

        public TrampolineException(CallTarget target, Object[] args) {
            this.target = target;
            this.args = args;
        }
    }
}
