package com.github.overmind.yasir;

import com.github.overmind.yasir.value.Box;
import com.github.overmind.yasir.value.Closure;
import com.github.overmind.yasir.value.Nil;
import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem(value = {long.class, boolean.class, Symbol.class, Closure.class, Box.class, Nil.class})
public abstract class YasirTypes {
}
