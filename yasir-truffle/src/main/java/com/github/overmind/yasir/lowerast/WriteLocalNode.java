package com.github.overmind.yasir.lowerast;

import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.value.Nil;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild(value = "value")
public abstract class WriteLocalNode extends Expr {
    @Specialization
    protected Object doWrite(VirtualFrame frame, FrameSlot slot, Object value) {
        frame.setObject(slot, value);
        return Nil.INSTANCE;
    }
}
