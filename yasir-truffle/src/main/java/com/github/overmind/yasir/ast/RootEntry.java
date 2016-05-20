package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public final class RootEntry {
    public static RootNode create(Expr body) {
        return create(body, null);
    }

    public static RootNode create(Expr body, FrameDescriptor fd) {
        return new RootEntryImpl(fd, body);
    }

    protected static final class RootEntryImpl extends RootNode {
        @Child
        protected Expr body;

        protected RootEntryImpl(FrameDescriptor frameDescriptor, Expr body) {
            super(Yasir.getLanguageClass(), null, frameDescriptor);

            this.body = body;
        }

        public String toString() {
            return "Entry(" + body + ")";
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }
}
