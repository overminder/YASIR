package com.github.overmind.yasir.ast;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

// The specialization must match the write node's impl.
@NodeField(name = "slot", type = FrameSlot.class)
public abstract class ReadLocalNode extends Expr {
    abstract FrameSlot getSlot();

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
