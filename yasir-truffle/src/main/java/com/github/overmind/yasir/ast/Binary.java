package com.github.overmind.yasir.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

@NodeChildren({@NodeChild("lhs"), @NodeChild("rhs")})
public abstract class Binary extends Expr {
}