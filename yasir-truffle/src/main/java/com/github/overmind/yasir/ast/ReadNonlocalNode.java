package com.github.overmind.yasir.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild("which")
public abstract class ReadNonlocalNode extends Expr {
    abstract FrameSlot getSlot();

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected long readLong(MaterializedFrame frame)
            throws FrameSlotTypeException {
        return frame.getLong(getSlot());
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected boolean readBoolean(MaterializedFrame frame)
            throws FrameSlotTypeException {
        return frame.getBoolean(getSlot());
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected Object readObject(MaterializedFrame frame)
            throws FrameSlotTypeException {
        return frame.getObject(getSlot());
    }

    @Specialization(contains = {"readLong", "readBoolean", "readObject"})
    public Object read(MaterializedFrame frame) {
        return frame.getValue(getSlot());
    }
}

