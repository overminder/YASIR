package com.github.overmind.yasir.value;

import com.oracle.truffle.api.frame.MaterializedFrame;

public class Closure {
    public final BareFunction bareFunction;
    private final Object payloads;

    public Closure(BareFunction bareFunction, Object[] payloads) {
        this.bareFunction = bareFunction;
        this.payloads = payloads;
    }

    public Object[] payloadsAsArray() {
        return (Object[]) payloads;
    }

    public MaterializedFrame payloadsAsMatFrame() {
        return (MaterializedFrame) payloads;
    }
}
