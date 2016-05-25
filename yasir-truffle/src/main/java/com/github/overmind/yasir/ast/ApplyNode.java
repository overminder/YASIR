package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.interp.InterpException;
import com.github.overmind.yasir.value.BareFunction;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;

public final class ApplyNode {
    public static Expr known(BareFunction func, Expr... args) {
        return new KnownApplyNode(func, args);
    }

    public static Expr knownTail(BareFunction func, Expr... args) {
        return new KnownApplyNode(func, true, args);
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
            return dispatchNode.executeDispatch(frame,
                    (BareFunction) func.executeGeneric(frame),
                    evalArgs(frame, args));
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
            funcValue = (Closure) func.executeGeneric(frame);
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

        final boolean tail;

        private final BranchProfile tailTaken = BranchProfile.create();

        public KnownApplyNode(BareFunction func, Expr... args) {
            this(func, false, args);
        }

        public KnownApplyNode(BareFunction func, boolean tail, Expr... args) {
            this.func = func;
            this.args = args;
            this.tail = tail;
        }

        @Override
        // @ExplodeLoop
        public Object executeGeneric(VirtualFrame frame) {
            CompilerAsserts.compilationConstant(tail);
            CompilerAsserts.compilationConstant(func);

            BareFunction funcValue = func;
            Object[] argValues = evalArgs(frame, args);
            if (tail) {
                throw InterpException.tailCall(funcValue, argValues);
            }
            while (true) {
                try {
                    return dispatchNode.executeDispatch(frame, funcValue, argValues);
                } catch (InterpException.TrampolineException e) {
                    // tailTaken.enter();
                    funcValue = e.func;
                    argValues = e.args;
                }
            }
        }

    }
}
