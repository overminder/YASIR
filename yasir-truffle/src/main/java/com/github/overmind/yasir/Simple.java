package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.*;
import com.github.overmind.yasir.value.BareFunction;
import com.oracle.truffle.api.frame.*;

public class Simple {
    @SafeVarargs
    public static <A> A[] array(A... args) {
        return args;
    }

    public static Expr makeBench(int count, long warmupN, long n, BareFunction c) {
        FrameDescriptor mainFd = new FrameDescriptor();
        FrameSlot funcSlot = mainFd.addFrameSlot("funcSlot");
        FrameSlot nSlot = mainFd.addFrameSlot("n");
        Expr main = MkLambda.create("main", array(nSlot), array(funcSlot),
                Begin.create(
                        Vars.writeBox(funcSlot, PrimOp.lit(c)),

                        // Warmup. Loops needs 2 more times for OSR and the function body.
                        ApplyNode.unknown(Vars.readBox(funcSlot), PrimOp.lit(warmupN)),
                        ApplyNode.unknown(Vars.readBox(funcSlot), PrimOp.lit(warmupN)),
                        ApplyNode.unknown(Vars.readBox(funcSlot), PrimOp.lit(warmupN)),

                        // Actual bench.
                        PrimOp.bench(
                            ApplyNode.unknown(Vars.readBox(funcSlot), Vars.read(nSlot)),
                            count
                        )
                ),
                mainFd);

        return ApplyNode.unknown(main, PrimOp.lit(n));
    }

}
