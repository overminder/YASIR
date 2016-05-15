package com.github.overmind.yasir.test;

import com.github.overmind.yasir.Simple;
import com.github.overmind.yasir.ast.*;
import com.github.overmind.yasir.interp.Interp;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExprTest {
    private void assertEvaluatesTo(Expr expr, Object res) {
        assertEquals(Interp.run(expr), res);
    }

    @SafeVarargs
    private final <A> A[] array(A... args) {
        return args;
    }

    @Test
    public void testConst() {
        assertEvaluatesTo(Const.create(42), 42L);
    }

    @Test
    public void testAdd() {
        Expr expr = PrimOps.add(Const.create(40), Const.create(2));
        assertEvaluatesTo(expr, 42L);
    }

    @Test
    public void testLambda() {
        FrameDescriptor frameDescr = new FrameDescriptor();
        FrameSlot x = frameDescr.addFrameSlot("x");
        Expr id = MkLambda.create("id", array(x), Vars.read(x), frameDescr);
        Expr expr = Apply.create(id, Const.create(42));
        assertEvaluatesTo(expr, 42L);
    }

    @Test
    public void testLexicalScope() {
        FrameDescriptor outer = new FrameDescriptor();
        FrameDescriptor inner = new FrameDescriptor();
        FrameSlot x = outer.addFrameSlot("x");
        FrameSlot y = inner.addFrameSlot("y");
        Expr k = MkLambda.create("const", array(x),
                MkLambda.create("const-inner", array(y), Vars.read(x, 1), inner), outer);
        Expr expr = Apply.create(Apply.create(k, Const.create(42)), Const.create(0));
        assertEvaluatesTo(expr, 42L);
    }

    @Test
    public void testVarsWrite() {
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot x = fd.addFrameSlot("x");
        Expr fn = MkLambda.create("set-42", array(x),
                Begin.create(Vars.write(Const.create(42), x), Vars.read(x)), fd);
        Expr expr = Apply.create(fn, Const.create(0));
        assertEvaluatesTo(expr, 42L);
    }

    @Test
    public void testFibo() {
        assertEvaluatesTo(Simple.makeFibo(10), 55L);
    }
}
