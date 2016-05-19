package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.interp.InterpException;
import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.*;

final public class Apply {
    public static Expr create(Expr func, Expr... args) {
        return create(func, false, args);
    }

    public static Expr create(Expr func, boolean tail, Expr... args) {
        return new ApplyNew(func, args, tail);
    }

    static class ApplyNew extends Expr {
        @Child
        protected Expr func;

        @Children
        protected final Expr[] args;

        @Child
        protected Dispatch dispatch = ApplyFactory.DispatchNodeGen.create();

        private final boolean tail;

        public ApplyNew(Expr func, Expr[] args, boolean tail) {
            this.func = func;
            this.args = args;
            this.tail = tail;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            Closure funcValue = evalFunc(func, frame);
            Object[] argValues = evalArgs(funcValue.payload(), args, frame);
            return dispatch.executeDispatch(frame, funcValue, argValues);
        }
    }

    static class Indirect extends Expr {
        @Child
        protected Expr func;

        @Children
        protected final Expr[] args;

        @Child
        protected IndirectCallNode icallNode = Yasir.rt().createIndirectCallNode();

        private final boolean tail;

        public Indirect(Expr func, Expr[] args, boolean tail) {
            this.func = func;
            this.args = args;
            this.tail = tail;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            Closure funcValue = evalFunc(func, frame);
            // System.out.println("apply: " + funcValue + ", parent = " + getParent());
            return icallNode.call(frame, funcValue.target(), evalArgs(funcValue.payload(), args, frame));
        }
    }

    static class Uninitialized extends Expr {
        @Child
        protected Expr func;

        @Children
        protected final Expr[] args;

        private final boolean tail;

        public Uninitialized(Expr func, Expr[] args, boolean tail) {
            this.func = func;
            this.args = args;
            this.tail = tail;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            Closure funcValue = evalFunc(func, frame);
            CompilerDirectives.transferToInterpreter();
            Direct spec = new Direct(funcValue.target(), func, args, tail);
            // System.out.println("apply: " + funcValue + ", parent = " + getParent());
            return replace(spec).execute(frame, funcValue);
        }
    }

    static class Direct extends Expr {
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
        public Object executeGeneric(VirtualFrame frame) {
            return execute(frame, evalFunc(func, frame));
        }

        protected Object execute(VirtualFrame frame, Closure funcValue) {
            Object[] argValues = evalArgs(funcValue.payload(), args, frame);
            if (target.getCallTarget() != funcValue.target()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw InterpException.unexpected("Call not constant");
                // return icallNode.call(frame, funcValue.target(), argValues);
            } else {
                return target.call(frame, argValues);
            }
        }
    }

    private static Closure evalFunc(Expr func, VirtualFrame frame) {
        try {
            return func.executeClosure(frame);
        } catch (UnexpectedResultException e) {
            throw InterpException.unexpected(e);
        }
    }


    @ExplodeLoop
    private static Object[] evalArgs(Object payload, Expr[] args, VirtualFrame frame) {
        CompilerAsserts.compilationConstant(args.length);

        Object[] argValues = new Object[args.length + 1];
        argValues[0] = payload;
        for (int i = 0; i < args.length; ++i) {
            argValues[i + 1] = args[i].executeGeneric(frame);
        }
        return argValues;
    }

    abstract static class Dispatch extends Node {
        protected static final int INLINE_CACHE_SIZE = 2;
        public abstract Object executeDispatch(VirtualFrame frame, Closure funcValue, Object[] args);

        @Specialization(limit = "INLINE_CACHE_SIZE",
                guards = "funcValue == cached")
        protected static Object doDirect(VirtualFrame frame,
                                         Closure funcValue,
                                         Object[] args, //
                                         @Cached("funcValue") Closure cached, //
                                         @Cached("create(cached.target())") DirectCallNode callNode) {
            return callNode.call(frame, args);
        }

        @Specialization(contains = "doDirect")
        protected static Object doIndirect(VirtualFrame frame,
                                           Closure funcValue,
                                           Object[] args, //
                                           @Cached("create()") IndirectCallNode callNode) {
            return callNode.call(frame, funcValue.target(), args);
        }
    }
}

