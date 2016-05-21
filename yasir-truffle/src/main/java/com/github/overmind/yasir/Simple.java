package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.*;
import com.github.overmind.yasir.lowerast.*;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.frame.*;

public class Simple {
    @SafeVarargs
    public static <A> A[] array(A... args) {
        return args;
    }

    // A really minimal fibo that demonstrates the maximum performance Truffle can achieve.
    public static Expr makeFiboLower() {
        Closure fibo = FiboClosure.create();
        return new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                return fibo;
            }
        };
    }

    public static Expr makeFibo() {
        FrameDescriptor fiboFd = new FrameDescriptor();
        FrameSlot nSlot = fiboFd.addFrameSlot("n");
        FrameSlot fixSlot = fiboFd.addFrameSlot("fixSlot");

        return MkLambda.create("fibo", array(nSlot, fixSlot),
                array(),
                If.create(
                        PrimOps.lessThan(Vars.read(nSlot), Lit.create(2)),
                        Vars.read(nSlot),
                        PrimOps.add(
                                Apply.create(
                                        Vars.read(fixSlot),
                                        PrimOps.sub(Vars.read(nSlot), Lit.create(1)),
                                        Vars.read(fixSlot)),
                                Apply.create(
                                        Vars.read(fixSlot),
                                        PrimOps.sub(Vars.read(nSlot), Lit.create(2)),
                                        Vars.read(fixSlot)))),
                fiboFd);
    }

    public static Expr makeFiboBench(int count, long n) {
        FrameDescriptor mainFd = new FrameDescriptor();
        FrameSlot fiboSlot = mainFd.addFrameSlot("fiboSlot");
        FrameSlot nSlot = mainFd.addFrameSlot("n");
        Expr main = MkLambda.create("main", array(nSlot), array(fiboSlot),
                Begin.create(
                        Vars.write(makeFiboLower(), fiboSlot),
                        // Just for warmup.
                        Apply.create(Vars.readBox(fiboSlot), Lit.create(20), Vars.readBox(fiboSlot)),
                        PrimOps.bench(
                            Apply.create(Vars.readBox(fiboSlot), Vars.read(nSlot), Vars.readBox(fiboSlot)),
                            count
                        )
                ),
                mainFd);

        return Apply.create(main, Lit.create(n));
    }

}
