package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Lambda;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

final public class MkLambda {
    public static Expr create(String name, FrameSlot[] argNames, Expr body, FrameDescriptor fd) {
        return new Simple(name, fd, body, argNames);
    }

    static class Simple extends FramelessExpr {
        private final String name;

        @Child
        protected BodyWrapper bodyWrapper;

        private final RootCallTarget target;

        public Simple(String name, FrameDescriptor fd, Expr body, FrameSlot[] argNames) {
            this.name = name;
            this.bodyWrapper = new BodyWrapper(fd, body, argNames);
            target = Yasir.rt().createCallTarget(bodyWrapper);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return new Lambda(target, frame.materialize());
        }
    }

    static class BodyWrapper extends Expr {
        @Child
        protected Expr body;

        private final FrameSlot[] argNames;

        protected BodyWrapper(FrameDescriptor fd, Expr body, FrameSlot[] argNames) {
            super(fd);

            this.body = body;
            this.argNames = argNames;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            for (int i = 0; i < argNames.length; ++i) {
                frame.setObject(argNames[i], args[i + 1]);
            }
            return body.execute(frame);
        }
    }
}
