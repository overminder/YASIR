package com.github.overmind.yasir;

import com.github.overmind.yasir.Simple;
import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.interp.Interp;

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

    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]);
        Expr fiboN = Simple.makeFibo(n);
        for (int i = 0; i < 5; ++i) {
            bench("fibo " + n, () -> {
                Object res = Interp.run(fiboN);
                System.out.println("res = " + res);
            });
        }
    }

    // Explicit thunk since explicit is better than implicit.
    static void bench(String name, Runnable thunk) {
        long t0 = System.nanoTime();
        thunk.run();
        long t1 = System.nanoTime();
        System.out.printf("%s took %s millis\n", name, (t1 - t0) / 1000000.0);
    }
}
