package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.interp.InterpException;
import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

public class Vars {
    public static Expr write(Expr expr, FrameSlot slot) {
        return write(expr, slot, 0);
    }

    public static Expr write(Expr expr, FrameSlot slot, int depth) {
        return new FramelessExpr() {
            @Override
            public Object execute(VirtualFrame frame) {
                Yasir.atDepth(frame, depth).setObject(slot, expr.execute(frame));
                return Symbol.apply("#void");
            }
        };
    }

    public static Expr read(FrameSlot slot) {
        return read(slot, 0);
    }

    public static Expr read(FrameSlot slot, int depth) {
        return new FramelessExpr() {
            @Override
            public Object execute(VirtualFrame frame) {
                try {
                    return Yasir.atDepth(frame, depth).getObject(slot);
                } catch (FrameSlotTypeException e) {
                    throw InterpException.unexpected(e);
                }
            }
        };
    }
}
