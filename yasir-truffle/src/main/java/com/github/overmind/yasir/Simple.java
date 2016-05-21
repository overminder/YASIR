package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.*;
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

    public static Expr makeFiboBench(int count, long n) {
        FrameDescriptor mainFd = new FrameDescriptor();
        FrameSlot fiboSlot = mainFd.addFrameSlot("fiboSlot");
        FrameSlot nSlot = mainFd.addFrameSlot("n");
        Expr main = MkLambda.create("main", array(nSlot), array(fiboSlot),
                Begin.create(
                        Vars.writeBox(fiboSlot, makeFiboLower()),
                        // Just for warmup.
                        ApplyNode.unknown(Vars.readBox(fiboSlot), PrimOp.lit(20), Vars.readBox(fiboSlot)),
                        PrimOp.bench(
                            ApplyNode.unknown(Vars.readBox(fiboSlot), Vars.read(nSlot), Vars.readBox(fiboSlot)),
                            count
                        )
                ),
                mainFd);

        return ApplyNode.unknown(main, PrimOp.lit(n));
    }

}
