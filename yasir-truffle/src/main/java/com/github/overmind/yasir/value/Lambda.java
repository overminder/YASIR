package com.github.overmind.yasir.value;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;

public class Lambda extends Callable {
    private final RootCallTarget target;
    private final MaterializedFrame frame;

    public Lambda(RootCallTarget target, MaterializedFrame frame) {
        this.target = target;

        this.frame = frame;
    }

    @Override
    public Object payload() {
        return frame;
    }

    @Override
    public CallTarget target() {
        return target;
    }
}
