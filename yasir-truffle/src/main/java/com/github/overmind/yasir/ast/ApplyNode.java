package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.interp.InterpException;
import com.github.overmind.yasir.value.BareFunction;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public final class ApplyNode {
    public static Expr known(BareFunction func, Expr... args) {
        return new KnownApplyNode(func, args);
    }

    public static Expr unknown(Expr func, Expr... args) {
        return new UnknownApplyNode(func, args);
    }

    // And is tail call.
    public static Expr unknownWithPayload(Expr func, Expr... args) {
        return new UnknownApplyWithPayloadNode(func, args);
    }

    @ExplodeLoop
    static Object[] evalArgs(VirtualFrame frame, Expr[] args) {
        CompilerAsserts.compilationConstant(args.length);
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            values[i] = args[i].executeGeneric(frame);
        }
        return values;
    }

    @ExplodeLoop
    static Object[] evalArgs(VirtualFrame frame, Closure funcValue, Expr[] args) {
        CompilerAsserts.compilationConstant(args.length);
        Object[] values = new Object[args.length + 1];
        values[0] = funcValue;
        for (int i = 0; i < args.length; ++i) {
            values[i + 1] = args[i].executeGeneric(frame);
        }
        return values;
    }

    public static class UnknownApplyNode extends Expr {
        @Child
        private Expr func;

        @Children
        private final Expr[] args;

        @Child
        protected DispatchClosureNode dispatchNode = DispatchClosureNodeGen.create();

        public UnknownApplyNode(Expr func, Expr... args) {
            this.func = func;
            this.args = args;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            try {
                return dispatchNode.executeDispatch(frame,
                        func.executeBareFunction(frame),
                        evalArgs(frame, args));
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class UnknownApplyWithPayloadNode extends Expr {
        @Child
        private Expr func;

        @Children
        private final Expr[] args;

        @Child
        protected DispatchClosureNode dispatchNode = DispatchClosureNodeGen.create();

        public UnknownApplyWithPayloadNode(Expr func, Expr... args) {
            this.func = func;
            this.args = args;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            Closure funcValue;
            Object[] argValues;
            try {
                funcValue = func.executeClosure(frame);
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
            argValues = evalArgs(frame, funcValue, args);
            throw InterpException.tailCall(funcValue.bareFunction, argValues);
        }
    }

    public static class KnownApplyNode extends Expr {
        private final BareFunction func;

        @Children
        private final Expr[] args;

        @Child
        protected DispatchClosureNode dispatchNode = DispatchClosureNodeGen.create();

        public KnownApplyNode(BareFunction func, Expr... args) {
            this.func = func;
            this.args = args;
        }

        @Override
        @ExplodeLoop
        public Object executeGeneric(VirtualFrame frame) {
            BareFunction funcValue = func;
            Object[] argValues;
            argValues = evalArgs(frame, args);
            while (true) {
                try {
                    return dispatchNode.executeDispatch(frame, funcValue, argValues);
                } catch (InterpException.TrampolineException e) {
                    funcValue = e.func;
                    argValues = e.args;
                }
            }
        }

    }
}
