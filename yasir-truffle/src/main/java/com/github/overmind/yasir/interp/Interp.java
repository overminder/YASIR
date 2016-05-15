package com.github.overmind.yasir.interp;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.ast.RootEntry;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.DirectCallNode;

public final class Interp {
    public static Object run(Expr e) {
        return Yasir.rt().createCallTarget(RootEntry.create(e)).call();
    }
}
