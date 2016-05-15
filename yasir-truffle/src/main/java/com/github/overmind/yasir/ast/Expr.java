package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Callable;
import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import com.github.overmind.yasir.YasirTypesGen;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public abstract class Expr extends RootNode {
    protected Expr(FrameDescriptor fd) {
        super(Yasir.getLanguageClass(), null, fd);
    }

    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return YasirTypesGen.expectLong(execute(frame));
    }

    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return YasirTypesGen.expectBoolean(execute(frame));
    }

    public Callable executeCallable(VirtualFrame frame) throws UnexpectedResultException {
        return YasirTypesGen.expectCallable(execute(frame));
    }

    public Symbol executeSymbol(VirtualFrame frame) throws UnexpectedResultException {
        return YasirTypesGen.expectSymbol(execute(frame));
    }
}
