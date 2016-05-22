package com.github.overmind.yasir.value;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.utilities.CyclicAssumption;

public final class Closure {
    private CallTarget target;
    private final String name;
    private final CyclicAssumption targetNotChanged;
    private final Object payload;

    public static Closure empty(String name) {
        return new Closure(null, name);
    }

    public Closure(CallTarget target, String name) {
        this(target, name, Nil.INSTANCE);
    }

    private Closure(CallTarget target, String name, Object payload) {
        this.target = target;
        this.name = name;
        targetNotChanged = new CyclicAssumption("Closure " + name + " not changed");
        this.payload = payload;
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

    public String name() {
        return name;
    }

    public Object payload() {
        return payload;
    }

    public Closure withPayloads(Object[] payloads) {
        return new Closure(target, name, payloads);
    }
}
