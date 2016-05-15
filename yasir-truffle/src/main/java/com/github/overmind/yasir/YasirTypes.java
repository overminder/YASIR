package com.github.overmind.yasir;

import com.github.overmind.yasir.value.Callable;
import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem(value = {long.class, boolean.class, Symbol.class, Callable.class})
public abstract class YasirTypes {
}
