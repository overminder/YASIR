package com.github.overmind.yasir.lowerast;

import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public final class ApplyNode {
    static Expr known(Closure func, Expr... args) {
        return new KnownApplyNode(func, args);
    }

    static Expr unknown(Expr func, Expr... args) {
        return new UnknownApplyNode(func, args);
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
                        func.executeClosure(frame),
                        evalArgs(frame, args));
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class KnownApplyNode extends Expr {
        private final Closure func;

        @Children
        private final Expr[] args;

        @Child
        protected DispatchClosureNode dispatchNode = DispatchClosureNodeGen.create();

        public KnownApplyNode(Closure func, Expr... args) {
            this.func = func;
            this.args = args;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return dispatchNode.executeDispatch(frame, func, evalArgs(frame, args));
        }

    }
}
