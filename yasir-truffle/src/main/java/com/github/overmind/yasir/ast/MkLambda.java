package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Lambda;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

final public class MkLambda {
    public static Expr create(String name, FrameSlot[] argNames, Expr body, FrameDescriptor fd) {
        return new Info(name, fd, body, argNames);
    }

    public static class Info extends Expr {
        public final String name;

        @Child protected BodyWrapper bodyWrapper;

        public final RootCallTarget target;

        public Info(String name, FrameDescriptor fd, Expr body, FrameSlot[] argNames) {
            this.name = name;
            bodyWrapper = new BodyWrapper(body, argNames);
            target = Yasir.rt().createCallTarget(RootEntry.create(bodyWrapper, fd));
        }

        @Override
        public String toString() {
            return "#<MkLambda " + name + ">";
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return new Lambda(this, frame.materialize());
        }
    }

    static class BodyWrapper extends Expr {
        @Child
        protected Expr body;

        private final FrameSlot[] argNames;

        protected BodyWrapper(Expr body, FrameSlot[] argNames) {
            this.body = body;
            this.argNames = argNames;
        }

        @Override
        @ExplodeLoop
        public Object executeGeneric(VirtualFrame frame) {
            CompilerAsserts.compilationConstant(argNames.length);

            Object[] args = frame.getArguments();
            for (int i = 0; i < argNames.length; ++i) {
                frame.setObject(argNames[i], args[i + 1]);
            }
            return body.executeGeneric(frame);
        }
    }
}
