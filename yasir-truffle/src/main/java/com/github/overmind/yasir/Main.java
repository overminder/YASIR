package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.ast.RootEntry;
import com.github.overmind.yasir.ast.TestFiboClosure;
import com.github.overmind.yasir.ast.TestLoopClosure;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.lang.management.ManagementFactory;
import java.util.List;


public class Main {
    static void printVmArgs() {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : args) {
            System.out.printf("%s, ", arg);
        }
        System.out.println();
    }

    static void bench() {
        Expr fiboN = Simple.makeBench(
                1, // count
                /*
                100000, // warmup
                100000000, // arg
                TestLoopClosure.create()
                */
                20, // warmup
                30, // arg
                TestFiboClosure.create()
        );
        Yasir.rt().createCallTarget(RootEntry.create(fiboN)).call();
    }

    public static void main(String[] args) {
        bench();
    }
}
