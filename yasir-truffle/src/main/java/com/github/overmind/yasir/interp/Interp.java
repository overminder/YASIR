package com.github.overmind.yasir.interp;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.ast.Expr;

public final class Interp {
    public static Object run(Expr e) {
        return Yasir.rt().createCallTarget(e).call();
    }
}
