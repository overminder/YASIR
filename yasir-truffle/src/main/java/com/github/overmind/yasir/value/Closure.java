package com.github.overmind.yasir.value;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.ast.MkLambda;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import static java.awt.SystemColor.info;

public final class Closure {
    private CallTarget target;
    private final String name;
    private final CyclicAssumption targetNotChanged;

    public Closure(CallTarget target, String name) {
        this.target = target;
        this.name = name;
        targetNotChanged = new CyclicAssumption("Closure " + name + " not changed");
    }

    @Override
    public String toString() {
        return "#<Closure " + name + ">";
    }

    public CallTarget target() {
        return target;
    }

    public Assumption targetNotChanged() {
        return targetNotChanged.getAssumption();
    }

    public void setTarget(RootCallTarget target) {
        this.target = target;
        targetNotChanged.invalidate();
    }
}
