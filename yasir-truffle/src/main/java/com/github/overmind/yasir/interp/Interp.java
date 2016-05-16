package com.github.overmind.yasir.interp;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.ast.Begin;
import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.ast.RootEntry;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import java.util.stream.IntStream;

public final class Interp {
    public static Object run(Expr e) {
        return Yasir.rt().createCallTarget(RootEntry.create(e)).call();
    }


    public static void bench(String name, Expr toCall, int nTimes) {
        RootCallTarget target = Yasir.rt().createCallTarget(RootEntry.create(toCall));
        IntStream.range(1, nTimes).forEach(i -> {
            benchOnce("name", () -> {
                Object res = target.call();
                System.out.println("res = " + res);
            });
        });
    }

    // Explicit thunk since explicit is better than implicit.
    // ^ The comment was made when I was using Scala.
    static void benchOnce(String name, Runnable thunk) {
        long t0 = System.nanoTime();
        thunk.run();
        long t1 = System.nanoTime();
        System.out.printf("%s took %s millis\n", name, (t1 - t0) / 1000000.0);
    }
}
