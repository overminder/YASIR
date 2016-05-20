package com.github.overmind.yasir;

import com.github.overmind.yasir.ast.*;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

public class Simple {
    @SafeVarargs
    public static <A> A[] array(A... args) {
        return args;
    }

    static abstract class DispatchClosureNode extends Node {
        protected static final int INLINE_CACHE_SIZE = 2;

        public abstract Object executeDispatch(VirtualFrame frame,
                                               Closure function,
                                               Object[] arguments);

        @Specialization(limit = "INLINE_CACHE_SIZE", guards = "function == cachedFunction", assumptions = "cachedFunction.targetNotChanged()")
        protected static Object doDirect(VirtualFrame frame, Closure function, Object[] arguments, //
                                         @Cached("function") Closure cachedFunction, //
                                         @Cached("create(cachedFunction.target())") DirectCallNode callNode) {
        /* Inline cache hit, we are safe to execute the cached call target. */
            return callNode.call(frame, arguments);
        }

        /**
         * Slow-path code for a call, used when the polymorphic inline cache exceeded its maximum size
         * specified in <code>INLINE_CACHE_SIZE</code>. Such calls are not optimized any further, e.g.,
         * no method inlining is performed.
         */
        @Specialization(contains = "doDirect")
        protected static Object doIndirect(VirtualFrame frame, Closure function, Object[] arguments, //
                                           @Cached("create()") IndirectCallNode callNode) {
        /*
         * SL has a quite simple call lookup: just ask the function for the current call target, and
         * call it.
         */
            return callNode.call(frame, function.target(), arguments);
        }
    }

    public static Expr makeFibo() {
        Closure fibo = new Closure(null, "fibo-closure");
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot n = fd.addFrameSlot("n", FrameSlotKind.Long);
        RootNode root = RootEntry.create(new Expr() {
            @Child DispatchClosureNode dcn1 = SimpleFactory.DispatchClosureNodeGen.create();
            @Child DispatchClosureNode dcn2 = SimpleFactory.DispatchClosureNodeGen.create();

            long readArg(VirtualFrame frame) {
                return (long) frame.getArguments()[0];
            }

            long readN(VirtualFrame frame) {
                try {
                    return frame.getLong(n);
                } catch (FrameSlotTypeException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Object executeGeneric(VirtualFrame frame) {
                frame.setLong(n, readArg(frame));

                if (readN(frame) < 2) {
                    return readN(frame);
                } else {
                    return (Long) dcn1.executeDispatch(frame, fibo, new Object[]{readN(frame) - 1}) +
                           (Long) dcn2.executeDispatch(frame, fibo, new Object[]{readN(frame) - 2});
                }
            }
        }, fd);

        fibo.setTarget(Yasir.rt().createCallTarget(root));

        return new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                return fibo;
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
                        Apply.create(Vars.readBox(fiboSlot), Lit.create(20), Vars.readBox(fiboSlot))
                        // PrimOps.bench(
                        //     Apply.create(Vars.readBox(fiboSlot), Vars.read(nSlot), Vars.readBox(fiboSlot)),
                        //     count
                        // )
                ),
                mainFd);

        return Apply.create(main, Lit.create(n));
    }
}
