package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

public class Simple {
    @SafeVarargs
    public static <A> A[] array(A... args) {
        return args;
    }

    public static Expr makeFibo(long n) {
        FrameDescriptor mainFd = new FrameDescriptor();
        FrameDescriptor fiboFd = new FrameDescriptor();
        FrameSlot fiboSlot = mainFd.addFrameSlot("fiboSlot");
        FrameSlot nSlot = fiboFd.addFrameSlot("n");

        Expr fiboBody = MkLambda.create("fibo", array(nSlot),
                array(),
                If.create(
                        PrimOps.lessThan(Vars.read(nSlot), Lit.create(2)),
                        Vars.read(nSlot),
                        PrimOps.add(
                                Apply.create(Vars.readBox(fiboSlot, 1), PrimOps.sub(Vars.read(nSlot), Lit.create(1))),
                                Apply.create(Vars.readBox(fiboSlot, 1), PrimOps.sub(Vars.read(nSlot), Lit.create(2))))),
                fiboFd);

        Expr main = MkLambda.create("main", array(), array(fiboSlot),
                Begin.create(
                        Vars.write(fiboBody, fiboSlot),
                        Apply.create(Vars.readBox(fiboSlot), Lit.create(n))
                ),
                mainFd);

        return Apply.create(main);
    }
}
