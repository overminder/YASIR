package com.github.overmind.yasir.value;

import com.github.overmind.yasir.ast.MkLambda;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;

public class Closure {
    private final MaterializedFrame frame;
    private final MkLambda.Info info;

    public Closure(MkLambda.Info info, MaterializedFrame frame) {
        this.info = info;
        this.frame = frame;
    }

    @Override
    public String toString() {
        return "#<Closure " + info.name + ">";
    }

    public Object payload() {
        return frame;
    }

    public CallTarget target() {
        return info.target;
    }
}
