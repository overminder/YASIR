package com.github.overmind.yasir.test;

import com.github.overmind.yasir.Simple;
import com.github.overmind.yasir.ast.*;
import com.github.overmind.yasir.interp.Interp;
import com.github.overmind.yasir.value.Box;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import org.junit.Test;

import static com.github.overmind.yasir.Simple.array;
import static org.junit.Assert.assertEquals;

public class ExprTest {
    private void assertEvaluatesTo(Expr expr, Object res) {
        assertEquals(Interp.run(expr), res);
    }

    private void assertEvaluatesToBoxed(Expr expr, Object res) {
        assertEquals(((Box) Interp.run(expr)).value(), res);
    }

    @Test
    public void testConst() {
        assertEvaluatesTo(Lit.create(42), 42L);
    }

    @Test
    public void testAdd() {
        Expr expr = PrimOps.add(Lit.create(40), Lit.create(2));
        assertEvaluatesTo(expr, 42L);
    }

    @Test
    public void testLambda() {
        FrameDescriptor frameDescr = new FrameDescriptor();
        FrameSlot x = frameDescr.addFrameSlot("x");
        Expr id = MkLambda.create("id", array(x), array(), Vars.read(x), frameDescr);
        Expr expr = Apply.create(id, Lit.create(42));
        assertEvaluatesTo(expr, 42L);
    }

    @Test
    public void testLexicalScope() {
        FrameDescriptor outer = new FrameDescriptor();
        FrameDescriptor inner = new FrameDescriptor();
        FrameSlot x = outer.addFrameSlot("x");
        FrameSlot y = inner.addFrameSlot("y");
        Expr k = MkLambda.create("const", array(x), array(),
                MkLambda.create("const-inner", array(y), array(), Vars.read(x, 1), inner), outer);
        Expr expr = Apply.create(Apply.create(k, Lit.create(42)), Lit.create(0));
        assertEvaluatesTo(expr, 42L);
    }

    @Test
    public void testVarsWrite() {
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot x = fd.addFrameSlot("x");
        Expr fn = MkLambda.create("set-42", array(x), array(),
                Begin.create(Vars.write(Lit.create(42), x), Vars.read(x)), fd);
        Expr expr = Apply.create(fn, Lit.create(Box.create()));
        assertEvaluatesToBoxed(expr, 42L);
    }

    @Test
    public void testFibo() {
        assertEvaluatesTo(Simple.makeFibo(10), 55L);
    }
}
