package com.github.overmind.yasir.interp;

import com.github.overmind.yasir.value.Closure;

public final class InterpException {
    public static RuntimeException unexpected(Exception e) {
        return new UnexpectedInterpException(e);
    }

    public static RuntimeException unexpected(String msg) {
        return new UnexpectedInterpException(new Exception(msg));
    }

    public static TrampolineException tailCall(Closure func, Object[] args) {
        return new TrampolineException(func, args);
    }

    static class UnexpectedInterpException extends RuntimeException {
        public final Exception inner;

        UnexpectedInterpException(Exception e) {
            inner = e;
        }
    }

    public static class TrampolineException extends RuntimeException {
        public final Closure func;
        public final Object[] args;

        public TrampolineException(Closure target, Object[] args) {
            this.func = target;
            this.args = args;
        }
    }
}
