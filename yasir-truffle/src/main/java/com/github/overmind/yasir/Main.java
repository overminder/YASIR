package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.ast.RootEntry;
import com.github.overmind.yasir.interp.Interp;
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

    static void benchFibo(long n) {
        Expr fiboN = Simple.makeFiboBench(0 /* count */, n);
        Yasir.rt().createCallTarget(RootEntry.create(fiboN)).call();
    }

    public static void main(String[] args) {
        try {
            benchFibo(40);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof UnexpectedResultException) {
                e.printStackTrace();
                e.getCause().printStackTrace();
                System.out.println("unexpected result: " + ((UnexpectedResultException) e.getCause()).getResult());
            }
        }
    }
}
