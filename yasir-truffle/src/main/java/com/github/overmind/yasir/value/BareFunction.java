package com.github.overmind.yasir.value;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.utilities.CyclicAssumption;

public final class BareFunction {
    private CallTarget target;
    private final String name;
    private final CyclicAssumption targetNotChanged;

    public static BareFunction empty(String name) {
        return new BareFunction(null, name);
    }

    public BareFunction(CallTarget target, String name) {
        this.target = target;
        this.name = name;
        targetNotChanged = new CyclicAssumption("BareFunction " + name + " not changed");
    }

    @Override
    public String toString() {
        return "#<BareFunction " + name + ">";
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

    public String name() {
        return name;
    }

    public Closure withPayloads(Object[] payloads) {
        return new Closure(this, payloads);
    }
}
