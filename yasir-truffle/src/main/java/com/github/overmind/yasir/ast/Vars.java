package com.github.overmind.yasir.ast;

import com.oracle.truffle.api.frame.FrameSlot;

public final class Vars {
    public static Expr read(FrameSlot slot) {
        return ReadLocalNodeGen.create(slot);
    }

    public static Expr readBox(FrameSlot slot) {
        return PrimOp.readBox(read(slot));
    }

    public static Expr write(FrameSlot slot, Expr value) {
        return WriteLocalNodeGen.create(value, slot);
    }

    public static Expr writeBox(FrameSlot slot, Expr value) {
        return PrimOp.writeBox(read(slot), value);
    }
}
