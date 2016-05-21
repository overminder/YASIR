package com.github.overmind.yasir.test;

import com.github.overmind.yasir.interp.Interp;
import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.value.Box;

import static org.junit.Assert.assertEquals;

public class ExprTest {
    private void assertEvaluatesTo(Expr expr, Object res) {
        assertEquals(Interp.run(expr), res);
    }

    private void assertEvaluatesToBoxed(Expr expr, Object res) {
        assertEquals(((Box) Interp.run(expr)).value(), res);
    }
}
