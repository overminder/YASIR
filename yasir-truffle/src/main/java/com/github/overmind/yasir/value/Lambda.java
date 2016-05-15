package com.github.overmind.yasir.value;

import com.github.overmind.yasir.ast.MkLambda;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;

public class Lambda extends Callable {
    private final MaterializedFrame frame;
    private final MkLambda.Info info;

    public Lambda(MkLambda.Info info, MaterializedFrame frame) {
        this.info = info;
        this.frame = frame;
    }

    @Override
    public String toString() {
        return "#<Lambda " + info.name + ">";
    }

    @Override
    public Object payload() {
        return frame;
    }

    @Override
    public CallTarget target() {
        return info.target;
    }
}
