package com.github.overmind.yasir;

import com.github.overmind.yasir.value.*;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem(value = {long.class, boolean.class, Symbol.class, BareFunction.class, Closure.class, Box.class, Nil.class, Object[].class})
public abstract class YasirTypes {
}
