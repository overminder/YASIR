package com.github.overmind.yasir.lowerast;

import com.github.overmind.yasir.ast.Expr;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "ix", type = int.class)
public abstract class ReadArgNode extends Expr {
    abstract int getIx();

    @Specialization
    public Object read(VirtualFrame frame) {
        return frame.getArguments()[getIx()];
    }
}
