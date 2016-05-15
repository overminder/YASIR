package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.interp.InterpException;
import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Callable;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

final public class Apply {
    public static Expr create(Expr func, Expr... args) {
        return create(func, false, args);
    }

    public static Expr create(Expr func, boolean tail, Expr... args) {
        return new Uninitialized(func, args, tail);
    }

    static class Uninitialized extends FramelessExpr {
        @Child
        protected Expr func;

        @Children
        protected final Expr[] args;

        @Child
        protected IndirectCallNode icallNode = Yasir.rt().createIndirectCallNode();

        private final boolean tail;

        public Uninitialized(Expr func, Expr[] args, boolean tail) {
            this.func = func;
            this.args = args;
            this.tail = tail;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (getParent() != null) {
                CompilerDirectives.transferToInterpreter();
                Callable funcValue = evalFunc(func, frame);
                Direct spec = new Direct(funcValue.target(), func, args, tail);
                return replace(spec).execute(frame, funcValue);
            } else {
                Callable funcValue = evalFunc(func, frame);

                return icallNode.call(frame, funcValue.target(), evalArgs(funcValue.payload(), args, frame));
            }
        }
    }

    static class Direct extends FramelessExpr {
        @Child
        protected Expr func;

        @Child
        protected DirectCallNode target;

        private final IndirectCallNode icallNode = Yasir.rt().createIndirectCallNode();

        @Children
        protected final Expr[] args;

        private final boolean tail;

        public Direct(CallTarget cachedTarget, Expr func, Expr[] args, boolean tail) {
            this.target = Yasir.rt().createDirectCallNode(cachedTarget);
            this.func = func;
            this.args = args;
            this.tail = tail;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return execute(frame, evalFunc(func, frame));
        }

        protected Object execute(VirtualFrame frame, Callable funcValue) {
            Object[] argValues = evalArgs(funcValue.payload(), args, frame);
            if (target.getCallTarget() != funcValue.target()) {
                return icallNode.call(frame, funcValue.target(), argValues);
            } else {
                return target.call(frame, argValues);
            }
        }
    }

    private static Callable evalFunc(Expr func, VirtualFrame frame) {
        try {
            return func.executeCallable(frame);
        } catch (UnexpectedResultException e) {
            throw InterpException.unexpected(e);
        }
    }


    @ExplodeLoop
    private static Object[] evalArgs(Object payload, Expr[] args, VirtualFrame frame) {
        Object[] argValues = new Object[args.length + 1];
        argValues[0] = payload;
        for (int i = 0; i < args.length; ++i) {
            argValues[i + 1] = args[i].execute(frame);
        }
        return argValues;
    }
}

