package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public final class FiboClosure {
    public static Closure create() {
        return createPassFuncBoxed();
    }

    public static Closure createFullyTruffled() {
        Closure fibo = new Closure(null, "fibo-closure");
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot n = fd.addFrameSlot("n", FrameSlotKind.Long);
        RootNode root = RootEntry.create(new FullyTruffled(n, fibo), fd);
        fibo.setTarget(Yasir.rt().createCallTarget(root));
        return fibo;
    }

    public static Closure createPassFunc() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new FiboClosure.PassFunc());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0), PrimOp.lit(fibo)));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    public static Closure createPassFuncBoxed() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new FiboClosure.PassFuncBoxed());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0), PrimOp.box(PrimOp.lit(fibo))));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    static class PassMoreFuncBoxed extends Expr {
        @Child
        private Expr body;

        PassMoreFuncBoxed() {
            Expr isBaseCase = PrimOp.callLt(readN(), PrimOp.lit(2));
            Expr recurNode = PrimOp.callAdd(
                    ApplyNode.unknown(readFiboUnbox(), PrimOp.callSub(readN(), PrimOp.lit(1)), readFibo()),
                    ApplyNode.unknown(readFiboUnbox(), PrimOp.callSub(readN(), PrimOp.lit(2)), readFibo()));
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readFiboUnbox() {
            return PrimOp.readBox(readFibo());
        }

        Expr readFibo() {
            return ReadArgNodeGen.create(1);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }

    // Passing the boxed function to the call site.
    static class PassFuncBoxed extends Expr {
        @Child
        private Expr body;

        PassFuncBoxed() {
            Expr isBaseCase = PrimOp.lt(readN(), PrimOp.lit(2));
            Expr recurNode = PrimOp.add(
                    ApplyNode.unknown(readFiboUnbox(), PrimOp.sub(readN(), PrimOp.lit(1)), readFibo()),
                    ApplyNode.unknown(readFiboUnbox(), PrimOp.sub(readN(), PrimOp.lit(2)), readFibo()));
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readFiboUnbox() {
            return PrimOp.readBox(readFibo());
        }

        Expr readFibo() {
            return ReadArgNodeGen.create(1);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }

    // Passing the function to the call site (i.e., fixpoint)
    static class PassFunc extends Expr {
        @Child
        private Expr body;

        PassFunc() {
            Expr isBaseCase = PrimOp.lt(readN(), PrimOp.lit(2));
            Expr recurNode = PrimOp.add(
                    ApplyNode.unknown(readFibo(), PrimOp.sub(readN(), PrimOp.lit(1)), readFibo()),
                    ApplyNode.unknown(readFibo(), PrimOp.sub(readN(), PrimOp.lit(2)), readFibo()));
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readFibo() {
            return ReadArgNodeGen.create(1);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }

    // Full truffle node with fixed call site.
    static class FullyTruffled extends Expr {
        final FrameSlot n;

        @Child
        private Expr populateFrame;

        @Child
        private Expr body;

        FullyTruffled(FrameSlot n, Closure fibo) {
            this.n = n;

            populateFrame = WriteLocalNodeGen.create(readArg(), n);
            Expr isBaseCase = PrimOp.lt(readLocal(), PrimOp.lit(2));
            Expr recurNode = PrimOp.add(
                    ApplyNode.unknown(PrimOp.lit(fibo), PrimOp.sub(readLocal(), PrimOp.lit(1))),
                    ApplyNode.unknown(PrimOp.lit(fibo), PrimOp.sub(readLocal(), PrimOp.lit(2))));
            body = new IfNode(isBaseCase, readLocal(), recurNode);
        }

        Expr readArg() {
            return ReadArgNodeGen.create(0);
        }

        Expr readLocal() {
            return ReadLocalNodeGen.create(n);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            populateFrame.executeGeneric(frame);
            return body.executeGeneric(frame);
        }
    }

    // Some of the constructs are in Java.
    static class Fast extends Expr {
        private final FrameSlot n;
        private final Closure fibo;
        @Child
        private WriteLocalNode populateFrame;
        @Child
        private Expr readArg;
        @Child
        private Expr readLocal;
        @Child
        private Expr lit1Node = PrimOp.lit(1);
        @Child
        private Expr lit2Node = PrimOp.lit(2);
        // @Child private Expr body;
        @Child
        private Expr isBaseCase;
        @Child
        private Expr recurNode;

        Fast(FrameSlot n, Closure fibo) {
            this.n = n;
            this.fibo = fibo;
            Expr loadFibo = PrimOp.lit(fibo);
            readArg = ReadArgNodeGen.create(0);
            readLocal = ReadLocalNodeGen.create(n);
            isBaseCase = PrimOp.lt(readLocal, lit2Node);
            recurNode = PrimOp.add(
                    ApplyNode.known(fibo, PrimOp.sub(readLocal, lit1Node)),
                    ApplyNode.known(fibo, PrimOp.sub(readLocal, lit2Node))
            );
            populateFrame = WriteLocalNodeGen.create(readArg, n);
        }

        long readArg(VirtualFrame frame) {
            try {
                return readArg.executeLong(frame);
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }

        long readN(VirtualFrame frame) {
            return (Long) readLocal.executeGeneric(frame);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            // frame.setLong(n, readN(frame));
            populateFrame.executeGeneric(frame);

            try {
                if (isBaseCase.executeBoolean(frame)) {
                    return readN(frame);
                } else {
                    return recurNode.executeGeneric(frame);
                }
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
