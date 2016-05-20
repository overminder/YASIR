package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Box;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

final public class MkLambda {
    public static Expr create(String name, FrameSlot[] argNames,
                              FrameSlot[] localNames, Expr body, FrameDescriptor fd) {
        return new Info(name, fd, body, argNames, localNames);
    }

    public static final class Info extends Expr {
        public final String name;

        // @Child protected BodyWrapper bodyWrapper;

        public final RootCallTarget target;

        public Info(String name, FrameDescriptor fd, Expr body,
                    FrameSlot[] argNames, FrameSlot[] localNames) {
            this.name = name;
            BodyWrapper bodyWrapper = new BodyWrapper(name, body, argNames, localNames);
            target = Yasir.rt().createCallTarget(RootEntry.create(bodyWrapper, fd));
        }

        @Override
        public String toString() {
            return "#<MkLambda " + name + ">";
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return new Closure(target, name);
        }
    }

    static class BodyWrapper extends Expr {
        private final String name;
        @Child
        protected Expr body;

        private final FrameSlot[] argNames;
        private final FrameSlot[] localNames;

        protected BodyWrapper(String name, Expr body, FrameSlot[] argNames, FrameSlot[] localNames) {
            this.name = name;
            this.body = body;
            this.argNames = argNames;
            this.localNames = localNames;
        }

        @Override
        public String toString() {
            return "#<LambdaBody " + name + ">";
        }

        @Override
        @ExplodeLoop
        public Object executeGeneric(VirtualFrame frame) {
            CompilerAsserts.compilationConstant(argNames.length);
            CompilerAsserts.compilationConstant(localNames.length);

            Object[] args = frame.getArguments();
            for (int i = 0; i < argNames.length; ++i) {
                frame.setObject(argNames[i], args[i]);
            }
            for (int i = 0; i < localNames.length; ++i) {
                frame.setObject(localNames[i], Box.create());
            }
            return body.executeGeneric(frame);
        }
    }
}
