package com.github.overmind.yasir.value;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.utilities.CyclicAssumption;

public class Box {
    public static Box create() {
        return create(null);
    }

    public static Box create(Object value) {
        Box b = new Box();
        b.value = value;
        return b;
    }

    public Object value() {
        if (assumption().isValid()) {
            // CompilerAsserts.compilationConstant(value);
            return value;
        } else {
            return value;
        }
    }

    public void setValue(Object newValue) {
        if (value != newValue) {
            value = newValue;
            immutable.invalidate();
        }
    }

    public Assumption assumption() { return immutable.getAssumption(); }


    private Object value;
    private final CyclicAssumption immutable = new CyclicAssumption("ImmutableBox");
}
