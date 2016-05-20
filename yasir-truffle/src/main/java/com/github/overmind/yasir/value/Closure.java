package com.github.overmind.yasir.value;

import com.github.overmind.yasir.ast.MkLambda;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;

import static java.awt.SystemColor.info;

public final class Closure {
    private final CallTarget target;
    private final String name;

    public Closure(CallTarget target, String name) {
        this.target = target;
        this.name = name;
    }

    @Override
    public String toString() {
        return "#<Closure " + name + ">";
    }

    public CallTarget target() {
        return target;
    }
}
