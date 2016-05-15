package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

public class Simple {
    public static Expr makeFibo(long n) {
        FrameDescriptor mainFd = new FrameDescriptor();
        FrameDescriptor fiboFd = new FrameDescriptor();
        FrameSlot fiboSlot = mainFd.addFrameSlot("fiboSlot");
        FrameSlot nSlot = fiboFd.addFrameSlot("n");

        Expr fiboBody = MkLambda.create("fibo", new FrameSlot[]{nSlot},
                If.create(
                        PrimOps.lessThan(Vars.read(nSlot), Const.create(2)),
                        Vars.read(nSlot),
                        PrimOps.add(
                                Apply.create(Vars.read(fiboSlot, 1), PrimOps.sub(Vars.read(nSlot), Const.create(1))),
                                Apply.create(Vars.read(fiboSlot, 1), PrimOps.sub(Vars.read(nSlot), Const.create(2))))),
                fiboFd);

        Expr main = MkLambda.create("main", new FrameSlot[]{},
                Begin.create(
                        Vars.write(fiboBody, fiboSlot),
                        Apply.create(Vars.read(fiboSlot), Const.create(n))),
                mainFd);

        return Apply.create(main);
    }
}
