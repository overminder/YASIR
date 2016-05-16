package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Box;
import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class Vars {
    public static Expr write(Expr expr, FrameSlot slot) {
        return write(expr, slot, 0);
    }

    public static Expr write(Expr expr, FrameSlot slot, int depth) {
        return new WriteBox(read(slot, depth), expr);
    }

    public static Expr read(FrameSlot slot) {
        return read(slot, 0);
    }

    public static Expr readBox(FrameSlot slot) {
        return readBox(slot, 0);
    }

    public static Expr readBox(FrameSlot slot, int depth) {
        return new ReadBox(read(slot, depth));
    }

    public static Expr read(FrameSlot slot, int depth) {
        if (depth == 0) {
            return doesNothing(VarsFactory.ReadLocalNodeGen.create(slot));
        } else {
            return VarsFactory.ReadLexicalNodeGen.create(slot, depth);
        }
    }

    // Does nothing but speeds up execution by 10x...
    static Expr doesNothing(Expr e0) {
        return new Expr() {
            @Child protected Expr e = e0;

            boolean initialized = false;
            Object v;

            @Override
            public Object executeGeneric(VirtualFrame frame) {
                if (initialized) {
                    return v;
                } else {
                    Object fresh = e.executeGeneric(frame);
                    v = fresh;
                    return fresh;
                }
            }
        };
    }

    static class WriteBox extends Expr {
        @Child
        private Expr box;

        @Child
        private Expr value;

        WriteBox(Expr box, Expr value) {
            this.box = box;
            this.value = value;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            ((Box) box.executeGeneric(frame)).setValue(value.executeGeneric(frame));
            return Symbol.apply("#void");
        }
    }

    static class ReadBox extends Expr {
        @Child
        private Expr box;

        ReadBox(Expr box) {
            this.box = box;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return ((Box) box.executeGeneric(frame)).value();
        }
    }

    @NodeFields(value={
            @NodeField(name = "slot", type = FrameSlot.class),
    })
    abstract static class ReadLocal extends Expr {
        protected abstract FrameSlot getSlot();

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected long readLong(VirtualFrame frame)
                throws FrameSlotTypeException {
            return frame.getLong(getSlot());
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected boolean readBoolean(VirtualFrame frame)
                throws FrameSlotTypeException {
            return frame.getBoolean(getSlot());
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected Object readObject(VirtualFrame frame)
                throws FrameSlotTypeException {
            return frame.getObject(getSlot());
        }

        @Specialization(contains = {"readLong", "readBoolean", "readObject"})
        public Object read(VirtualFrame frame) {
            return frame.getValue(getSlot());
        }
    }

    @NodeFields(value={
            @NodeField(name = "slot", type = FrameSlot.class),
            @NodeField(name = "depth", type = int.class),
    })
    abstract static class ReadLexical extends Expr {
        protected abstract FrameSlot getSlot();
        protected abstract int getDepth();

        static <A> A ensureConstant(A a) {
            CompilerAsserts.compilationConstant(a);
            return a;
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected long readLong(VirtualFrame frame)
                throws FrameSlotTypeException {
            MaterializedFrame there = Yasir.atDepth(frame, getDepth());
            return there.getLong(getSlot());
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected boolean readBoolean(VirtualFrame frame)
                throws FrameSlotTypeException {
            MaterializedFrame there = Yasir.atDepth(frame, getDepth());
            return there.getBoolean(getSlot());
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected Object readObject(VirtualFrame frame)
                throws FrameSlotTypeException {
            MaterializedFrame there = Yasir.atDepth(frame, getDepth());
            return there.getObject(getSlot());
        }

        @Specialization(contains = {"readLong", "readBoolean", "readObject"})
        public Object read(VirtualFrame frame) {
            MaterializedFrame there = Yasir.atDepth(frame, getDepth());
            return there.getValue(getSlot());
        }
    }
}
