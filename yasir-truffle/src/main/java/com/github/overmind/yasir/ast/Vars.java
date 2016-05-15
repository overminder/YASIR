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
        return new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                Object value = expr.executeGeneric(frame);
                if (depth == 0) {
                    frame.setObject(slot, value);
                } else {
                    Yasir.atDepth(frame, depth).setObject(slot, value);
                }
                return Symbol.apply("#void");
            }
        };
    }

    public static Expr read(FrameSlot slot) {
        return read(slot, 0);
    }

    public static Expr read(FrameSlot slot, int depth) {
        return new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                try {
                    if (depth == 0) {
                        return frame.getObject(slot);
                    } else {
                        return Yasir.atDepth(frame, depth).getObject(slot);
                    }
                } catch (FrameSlotTypeException e) {
                    throw InterpException.unexpected(e);
                }
            }
        };
    }
}
