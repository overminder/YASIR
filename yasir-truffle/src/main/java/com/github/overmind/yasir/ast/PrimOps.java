package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Closure;
import com.github.overmind.yasir.value.Symbol;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.*;

import static com.github.overmind.yasir.Simple.array;

final public class PrimOps {
    public static Expr add(Expr lhs, Expr rhs) {
        return PrimOpsFactory.AddNodeGen.create(lhs, rhs);
    }

    public static Expr println(Expr v) {
        return PrimOpsFactory.PrintLnNodeGen.create(array(v));
    }

    public static Expr bench(Expr v, int count) {
        return new Bench(v, count);
    }

    public static Expr sub(Expr lhs, Expr rhs) {
        return PrimOpsFactory.SubNodeGen.create(lhs, rhs);
    }

    public static Expr lessThan(Expr lhs, Expr rhs) {
        return PrimOpsFactory.LessThanNodeGen.create(lhs, rhs);
    }

    @NodeChild(value = "arguments", type = Expr[].class)
    abstract static class Builtin extends Expr {
    }

    @NodeInfo(shortName = "println")
    abstract static class PrintLn extends Builtin {
        @Specialization
        protected Symbol printLn(long value) {
            System.out.println(value);
            return Symbol.apply("#void");
        }
    }

    static final class Bench extends Expr {
        @Child private LoopNode loop;

        static final class LoopBody extends Node implements RepeatingNode {
            int nToGo;
            @Child Expr body;

            LoopBody(Expr body, int count) {
                this.body = body;
                this.nToGo = count;
            }

            @CompilerDirectives.TruffleBoundary
            static long nanoTime() {
                return System.nanoTime();
            }

            @CompilerDirectives.TruffleBoundary
            static void printResult(Object res, long t0) {
                System.out.println("Time used: " + (nanoTime() - t0) / 1000000 + ", res = " + res);
            }

            @Override
            public boolean executeRepeating(VirtualFrame frame) {
                if (nToGo <= 0) {
                    return false;
                } else {
                    nToGo -= 1;
                    long t0 = nanoTime();
                    printResult(body.executeGeneric(frame), t0);
                    return true;
                }
            }
        }

        Bench(Expr body, int count) {
            this.loop = Yasir.rt().createLoopNode(new LoopBody(body, count));
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            loop.executeLoop(frame);
            return Symbol.apply("#void");
        }
    }

    @NodeInfo(shortName = "+")
    abstract static class Add extends Binary {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected long add(long lhs, long rhs) {
            return lhs + rhs;
        }
    }

    @NodeInfo(shortName = "-")
    abstract static class Sub extends Binary {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected long sub(long lhs, long rhs) {
            return lhs - rhs;
        }
    }

    @NodeInfo(shortName = "<")
    abstract static class LessThan extends Binary {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected boolean lessThan(long lhs, long rhs) {
            return lhs < rhs;
        }
    }
}
