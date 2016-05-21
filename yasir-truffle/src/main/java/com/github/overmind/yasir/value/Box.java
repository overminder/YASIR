package com.github.overmind.yasir.value;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.utilities.CyclicAssumption;

public final class Box {
    private Object value;

    public Box(Object value) {
        this.value = value;
    }

    public static Box create() {
        return create(Nil.INSTANCE);
    }

    public static Box create(Object value) {
        Box b = new Box(value);
        return b;
    }

    public Object value() {
        return value;
    }

    public void setValue(Object newValue) {
        if (value != newValue) {
            value = newValue;
        }
    }
}
