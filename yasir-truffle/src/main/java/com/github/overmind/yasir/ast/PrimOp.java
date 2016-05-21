package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Box;
import com.github.overmind.yasir.value.Closure;
import com.github.overmind.yasir.value.Nil;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

public final class PrimOp {
    public static Expr add(Expr lhs, Expr rhs) {
        return PrimOpFactory.AddFactory.create(lhs, rhs);
    }

    public static Expr sub(Expr lhs, Expr rhs) {
        return PrimOpFactory.SubFactory.create(lhs, rhs);
    }

    public static Expr lt(Expr lhs, Expr rhs) {
        return PrimOpFactory.LtFactory.create(lhs, rhs);
    }

    public static Expr lit(long v) {
        return PrimOpFactory.LongLitNodeGen.create(v);
    }

    public static Expr lit(Closure c) {
        return PrimOpFactory.ClosureLitNodeGen.create(c);
    }

    public static Expr callAdd(Expr lhs, Expr rhs) {
        return makeBinaryCall(PrimOpFactory.AddFactory.getInstance(), lhs, rhs);
    }

    public static Expr callSub(Expr lhs, Expr rhs) {
        return makeBinaryCall(PrimOpFactory.SubFactory.getInstance(), lhs, rhs);
    }

    public static Expr callLt(Expr lhs, Expr rhs) {
        return makeBinaryCall(PrimOpFactory.LtFactory.getInstance(), lhs, rhs);
    }

    static Closure makeBinaryClosure(NodeFactory<? extends Expr> factory) {
        RootNode root = new RootNode(Yasir.getLanguageClass(), SourceSection.createUnavailable("builtin", null), null) {
            @Child
            private Expr body = factory.createNode(ReadArgNodeGen.create(0), ReadArgNodeGen.create(1));
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                return body.executeGeneric(frame);
            }
        };
        return new Closure(Yasir.rt().createCallTarget(root), factory.getNodeClass().getName());
    }

    static Expr makeBinaryCall(NodeFactory<? extends Expr> factory, Expr lhs, Expr rhs) {
        return ApplyNode.known(makeBinaryClosure(factory), lhs, rhs);
    }

    public static Expr box(Expr v) {
        return PrimOpFactory.MkBoxNodeGen.create(v);
    }

    public static Expr readBox(Expr v) {
        return PrimOpFactory.ReadBoxNodeGen.create(v);
    }

    public static Expr writeBox(Expr lhs, Expr rhs) {
        return PrimOpFactory.WriteBoxFactory.create(lhs, rhs);
    }

    public static Expr bench(Expr body, int count) {

        return new Bench(body, count);
    }

    @NodeField(name = "value", type = long.class)
    static abstract class LongLit extends Expr {
        @Specialization
        abstract long getValue();
    }

    @NodeField(name = "value", type = Closure.class)
    static abstract class ClosureLit extends Expr {
        @Specialization
        abstract Closure getValue();
    }

    @NodeChild("value")
    static abstract class MkBox extends Expr {
        @Specialization
        protected Object mk(Object value) {
            return new Box(value);
        }
    }

    @NodeChild("box")
    static abstract class ReadBox extends Expr {
        @Specialization
        protected Object read(Box box) {
            return box.value();
        }
    }

    static abstract class WriteBox extends Binary {
        @Specialization
        protected Object write(Box box, Object value) {
            box.setValue(value);
            return Nil.INSTANCE;
        }
    }

    @NodeChildren({@NodeChild("lhs"), @NodeChild("rhs")})
    @GenerateNodeFactory
    static abstract class Binary extends Expr {
    }

    static abstract class Add extends Binary {
        @Specialization
        protected long doLong(long lhs, long rhs) {
            return lhs + rhs;
        }
    }

    static abstract class Sub extends Binary {
        @Specialization
        protected long doLong(long lhs, long rhs) {
            return lhs - rhs;
        }
    }

    static abstract class Lt extends Binary {
        @Specialization
        protected boolean doLong(long lhs, long rhs) {
            return lhs < rhs;
        }
    }

    static final class Bench extends Expr {
        @Child private LoopNode loop;

        final static class LoopBody extends Node implements RepeatingNode {
            int nToGo;
            @Child Expr body;

            LoopBody(Expr body, int count) {
                this.body = body;
                this.nToGo = count;
            }

            @CompilerDirectives.TruffleBoundary
            long nanoTime() {
                return System.nanoTime();
            }

            @CompilerDirectives.TruffleBoundary
            void printResult(Object res, long t0) {
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
            return Nil.INSTANCE;
        }
    }
}
