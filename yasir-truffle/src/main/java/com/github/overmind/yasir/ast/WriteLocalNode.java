package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.value.Nil;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

// From SL.
@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild(value = "value")
public abstract class WriteLocalNode extends Expr {
    protected abstract FrameSlot getSlot();

    /**
     * Specialized method to write a primitive {@code long} value. This is only possible if the
     * local variable also has currently the type {@code long}, therefore a Truffle DSL
     * {@link #isLongKind(VirtualFrame) custom guard} is specified.
     */
    @Specialization(guards = "isLongKind(frame)")
    protected Object writeLong(VirtualFrame frame, long value) {
        frame.setLong(getSlot(), value);
        return Nil.INSTANCE;
    }

    @Specialization(guards = "isBooleanKind(frame)")
    protected Object writeBoolean(VirtualFrame frame, boolean value) {
        frame.setBoolean(getSlot(), value);
        return Nil.INSTANCE;
    }

    /**
     * Generic write method that works for all possible types.
     * <p>
     * Why is this method annotated with {@link Specialization} and not {@link Fallback}? For a
     * {@link Fallback} method, the Truffle DSL generated code would try all other specializations
     * first before calling this method. We know that all these specializations would fail their
     * guards, so there is no point in calling them. Since this method takes a value of type
     * {@link Object}, it is guaranteed to never fail, i.e., once we are in this specialization the
     * node will never be re-specialized.
     */
    @Specialization(contains = {"writeLong", "writeBoolean"})
    protected Object write(VirtualFrame frame, Object value) {
        if (getSlot().getKind() != FrameSlotKind.Object) {
            /*
             * The local variable has still a primitive type, we need to change it to Object. Since
             * the variable type is important when the compiler optimizes a method, we also discard
             * compiled code.
             */
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSlot().setKind(FrameSlotKind.Object);
        }
        frame.setObject(getSlot(), value);
        return Nil.INSTANCE;
    }

    /**
     * Guard function that the local variable has the type {@code long}.
     */
    @SuppressWarnings("unused")
    protected boolean isLongKind(VirtualFrame frame) {
        return isKind(FrameSlotKind.Long);
    }

    @SuppressWarnings("unused")
    protected boolean isBooleanKind(VirtualFrame frame) {
        return isKind(FrameSlotKind.Boolean);
    }

    private boolean isKind(FrameSlotKind kind) {
        if (getSlot().getKind() == kind) {
            /* Success: the frame slot has the expected kind. */
            return true;
        } else if (getSlot().getKind() == FrameSlotKind.Illegal) {
            /*
             * This is the first write to this local variable. We can set the type to the one we
             * expect. Since the variable type is important when the compiler optimizes a method, we
             * also discard compiled code.
             */
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSlot().setKind(kind);
            return true;
        } else {
            /*
             * Failure: the frame slot has the wrong kind, the Truffle DSL generated code will
             * choose a different specialization.
             */
            return false;
        }
    }
}
