package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.*;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;

public class Simple {
    @SafeVarargs
    public static <A> A[] array(A... args) {
        return args;
    }

    public static Expr makeFibo() {
        final RootCallTarget[] targetBox = new RootCallTarget[1];
        RootNode root = RootEntry.create(new Expr() {
            @Child IndirectCallNode dcn = Yasir.rt().createIndirectCallNode();
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                long n = (long) frame.getArguments()[0];

                if (n < 2) {
                    return n;
                } else {
                    return (Long) dcn.call(frame, targetBox[0], new Object[]{n - 1}) +
                            (Long) dcn.call(frame, targetBox[0], new Object[]{n - 2});
                }
            }
        });

        targetBox[0] = Yasir.rt().createCallTarget(root);

        return new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                return new Closure(targetBox[0], "fibo-java");
            }
        };
    }

    final static long fibo(long n) {
        if (n < 2) {
            return n;
        } else {
            return fibo(n - 1) + fibo(n - 2);
        }
    }

    public static Expr makeFibo2() {
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
                        Vars.write(makeFibo(), fiboSlot),
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
