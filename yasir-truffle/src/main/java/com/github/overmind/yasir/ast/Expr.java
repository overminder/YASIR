package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.YasirTypes;
import com.github.overmind.yasir.value.Closure;
import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import com.github.overmind.yasir.YasirTypesGen;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

@TypeSystemReference(YasirTypes.class)
public abstract class Expr extends Node {
    public abstract Object executeGeneric(VirtualFrame frame);

    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return YasirTypesGen.expectLong(executeGeneric(frame));
    }

    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return YasirTypesGen.expectBoolean(executeGeneric(frame));
    }

    public Closure executeClosure(VirtualFrame frame) throws UnexpectedResultException {
        return YasirTypesGen.expectClosure(executeGeneric(frame));
    }

    public Symbol executeSymbol(VirtualFrame frame) throws UnexpectedResultException {
        return YasirTypesGen.expectSymbol(executeGeneric(frame));
    }
}
