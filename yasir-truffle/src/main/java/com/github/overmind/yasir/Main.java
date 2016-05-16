package com.github.overmind.yasir;

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

    static void benchFibo(int n) {
        Expr fiboN = Simple.makeFibo(n);
        Interp.bench("fibo " + n, fiboN, 10);
    }

    public static void main(String[] args) {
        benchFibo(30);
    }
}
