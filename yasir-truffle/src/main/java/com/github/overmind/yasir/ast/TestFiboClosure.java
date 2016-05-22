package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Box;
import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import static com.github.overmind.yasir.Simple.array;

// Playing with different compilation strategies for global variables.
// What works: compilation constant, boxed compilation constant, pass in register (only for <= 4 args?)
// What doesn't: pass too many args in registers (4x slow down), pass in array (3x slow down), pass
// in materialized frame (7x slow down).
public final class TestFiboClosure {
    public static Closure create() {
        return createInjectBoxedClosuresThroughCompilationContext();
    }

    // Inlined primop nodes and constant closure pointer.
    // 1x speed
    public static Closure createFullyTruffled() {
        Closure fibo = new Closure(null, "fibo-closure");
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot n = fd.addFrameSlot("n", FrameSlotKind.Long);
        RootNode root = RootEntry.create(new FullyTruffled(n, fibo), fd);
        fibo.setTarget(Yasir.rt().createCallTarget(root));
        return fibo;
    }

    // Inlined primop nodes and passing fibo's closure pointer as argument.
    // 1x speed
    public static Closure createPassFunc() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new TestFiboClosure.PassFunc());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0), PrimOp.lit(fibo)));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Inlined primop nodes and passing fibo's closure pointer as boxed argument.
    // 1x speed
    public static Closure createPassFuncBoxed() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new TestFiboClosure.PassFuncBoxed());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0), PrimOp.box(PrimOp.lit(fibo))));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Passing all closure pointers (fibo, primops) as separate arguments.
    // 1/4x speed.
    public static Closure createPassMoreFunc() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new PassMoreFunc());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0),
                        PrimOp.lit(fibo),
                        PrimOp.lit(PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance())),
                        PrimOp.lit(PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance())),
                        PrimOp.lit(PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance()))));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Passing all closure pointers (fibo, primops) as an array argument.
    // 1/3x speed.
    public static Closure createPassMoreFuncInArray() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new PassMoreFuncInArray());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0),
                        PrimOp.lit(array(
                                fibo,
                                PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance()),
                                PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance()),
                                PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance())))));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Passing all closure pointers (fibo, primops) from the parent's frame.
    // 1/7x speed.
    public static Closure createPassMoreFuncInMaterializedFrame() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");
        FrameDescriptor trampoFd = new FrameDescriptor();

        FrameSlot fiboSlot = trampoFd.addFrameSlot("fibo");
        FrameSlot addSlot = trampoFd.addFrameSlot("add");
        FrameSlot subSlot = trampoFd.addFrameSlot("sub");
        FrameSlot ltSlot = trampoFd.addFrameSlot("lt");

        RootNode fiboRoot = RootEntry.create(new PassMoreFuncInMatFrame(fiboSlot, addSlot, subSlot, ltSlot));
        RootNode fiboTrampoRoot = RootEntry.create(Begin.create(
                Vars.write(fiboSlot, PrimOp.lit(fibo)),
                Vars.write(addSlot, PrimOp.lit(PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance()))),
                Vars.write(subSlot, PrimOp.lit(PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance()))),
                Vars.write(ltSlot, PrimOp.lit(PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance()))),
                ApplyNode.known(fibo, ReadArgNodeGen.create(0), PrimOp.matFrame())
        ), trampoFd);

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Inline all closure pointers (fibo, primops) in the code.
    // 1x speed.
    // This is what SL uses. And yes, this is among the fastest implementations.
    public static Closure createInjectClosuresThroughCompilationContext() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");

        Closure add = PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance());
        Closure sub = PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance());
        Closure lt = PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance());

        RootNode fiboRoot = RootEntry.create(
                new InjectClosureThroughCompilationContext(fibo, add, sub, lt));
        RootNode fiboTrampoRoot = RootEntry.create(ApplyNode.known(fibo, ReadArgNodeGen.create(0)));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }


    // Inline all closure pointers (fibo, primops) boxed in the code.
    // 1x speed.
    public static Closure createInjectBoxedClosuresThroughCompilationContext() {
        Closure fibo = new Closure(null, "fibo-closure");
        Closure fiboTrampo = new Closure(null, "fibo-trampo");

        Closure add = PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance());
        Closure sub = PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance());
        Closure lt = PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance());

        RootNode fiboRoot = RootEntry.create(
                new InjectClosureThroughCompilationContext(fibo, add, sub, lt));
        RootNode fiboTrampoRoot = RootEntry.create(ApplyNode.known(fibo, ReadArgNodeGen.create(0)));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Fast: inline primops with only the fibo function CPS-ed.
    static Expr createFastCPS() {
        Closure fiboEntryC = Closure.empty("fibo-entry"),
                fiboRet1C = Closure.empty("fibo-ret1"),
                fiboRet2C = Closure.empty("fibo-ret2");
        Expr fiboEntry, fiboRet1, fiboRet2;
        {
            fiboEntry = new Expr() {
                Expr isBaseCase = PrimOp.lt(readN(), PrimOp.lit(2));
                Expr nSub1 = PrimOp.sub(readN(), PrimOp.lit(1));
                Expr mkK = Closures.alloc(fiboRet1C, readN(), readK());
                Expr recurNode = ApplyNode.unknownWithPayload(PrimOp.lit(fiboEntryC), nSub1, mkK);
                @Child Expr body = new IfNode(isBaseCase,
                        ApplyNode.unknownWithPayload(readK(), readN()),
                        recurNode);

                private Expr readN() {
                    return Vars.read(0);
                }

                private Expr readK() {
                    return Vars.read(1);
                }

                @Override
                public Object executeGeneric(VirtualFrame frame) {
                    return body.executeGeneric(frame);
                }
            };
        }

        {
            fiboRet1 = new Expr() {
                Expr nSub2 = PrimOp.sub(readN(), PrimOp.lit(2));
                Expr mkK = Closures.alloc(fiboRet2C, readRes(), readK());
                Expr recurNode = ApplyNode.unknownWithPayload(PrimOp.lit(fiboEntryC), nSub2, mkK);
                @Child Expr body = recurNode;

                private Expr readClosure() {
                    return Vars.read(0);
                }

                private Expr readRes() {
                    return Vars.read(1);
                }

                private Expr readN() {
                    return PrimOp.readPayload(readClosure(), 0);
                }

                private Expr readK() {
                    return PrimOp.readPayload(readClosure(), 1);
                }

                @Override
                public Object executeGeneric(VirtualFrame frame) {
                    return body.executeGeneric(frame);
                }
            };
        }

        {
            fiboRet2 = new Expr() {
                Expr add = PrimOp.add(readRes(), readPrevRes());
                @Child Expr body = ApplyNode.unknownWithPayload(readK(), add);

                private Expr readClosure() {
                    return Vars.read(0);
                }

                private Expr readRes() {
                    return Vars.read(1);
                }

                private Expr readPrevRes() {
                    return PrimOp.readPayload(readClosure(), 0);
                }

                private Expr readK() {
                    return PrimOp.readPayload(readClosure(), 1);
                }

                @Override
                public Object executeGeneric(VirtualFrame frame) {
                    return body.executeGeneric(frame);
                }
            };
        }

        fiboEntryC.setTarget(Yasir.rt().createCallTarget(RootEntry.create(fiboEntry)));
        fiboRet1C.setTarget(Yasir.rt().createCallTarget(RootEntry.create(fiboRet1)));
        fiboRet2C.setTarget(Yasir.rt().createCallTarget(RootEntry.create(fiboRet2)));

        Closure id = new Closure(Yasir.rt().createCallTarget(RootEntry.create(Vars.read(0))), "id");
        Closure fibo = new Closure(Yasir.rt().createCallTarget(RootEntry.create(
                ApplyNode.known(fiboEntryC, Vars.read(0), PrimOp.lit(id)))),
                "fibo");

        return PrimOp.lit(fibo);
    }

    static class InjectBoxedClosureThroughCompilationContext extends Expr {
        @Child
        private Expr body;

        Box fibo;
        Box add;
        Box sub;
        Box lt;

        InjectBoxedClosureThroughCompilationContext(Closure fibo, Closure add, Closure sub, Closure lt) {
            this.fibo = new Box(fibo);
            this.add = new Box(add);
            this.sub = new Box(sub);
            this.lt = new Box(lt);

            Expr isBaseCase = ApplyNode.unknown(readLt(), readN(), PrimOp.lit(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(2));
            Expr ap1 = ApplyNode.unknown(readFibo(), nSub1);
            Expr ap2 = ApplyNode.unknown(readFibo(), nSub2);
            Expr recurNode = ApplyNode.unknown(readAdd(), ap1, ap2);
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readFibo() {
            return PrimOp.readBox(PrimOp.lit(fibo));
        }

        Expr readAdd() {
            return PrimOp.readBox(PrimOp.lit(add));
        }

        Expr readSub() {
            return PrimOp.readBox(PrimOp.lit(sub));
        }

        Expr readLt() {
            return PrimOp.readBox(PrimOp.lit(lt));
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }

    static class InjectClosureThroughCompilationContext extends Expr {
        @Child
        private Expr body;

        Closure fibo;
        Closure add;
        Closure sub;
        Closure lt;

        InjectClosureThroughCompilationContext(Closure fibo, Closure add, Closure sub, Closure lt) {
            this.fibo = fibo;
            this.add = add;
            this.sub = sub;
            this.lt = lt;

            Expr isBaseCase = ApplyNode.unknown(readLt(), readN(), PrimOp.lit(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(2));
            Expr ap1 = ApplyNode.unknown(readFibo(), nSub1);
            Expr ap2 = ApplyNode.unknown(readFibo(), nSub2);
            Expr recurNode = ApplyNode.unknown(readAdd(), ap1, ap2);
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readFibo() {
            return PrimOp.lit(fibo);
        }

        Expr readAdd() {
            return PrimOp.lit(add);
        }

        Expr readSub() {
            return PrimOp.lit(sub);
        }

        Expr readLt() {
            return PrimOp.lit(lt);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }

    static class PassMoreFuncInMatFrame extends Expr {
        @Child
        private Expr body;

        FrameSlot fibo;
        FrameSlot add;
        FrameSlot sub;
        FrameSlot lt;

        PassMoreFuncInMatFrame(FrameSlot fibo, FrameSlot add, FrameSlot sub, FrameSlot lt) {
            this.fibo = fibo;
            this.add = add;
            this.sub = sub;
            this.lt = lt;

            Expr isBaseCase = ApplyNode.unknown(readLt(), readN(), PrimOp.lit(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(2));
            Expr ap1 = ApplyNode.unknown(readFibo(), nSub1,
                    readPayload());
            Expr ap2 = ApplyNode.unknown(readFibo(), nSub2,
                    readPayload());
            Expr recurNode = ApplyNode.unknown(readAdd(), ap1, ap2);
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readPayload() {
            return ReadArgNodeGen.create(1);
        }

        Expr readFibo() {
            return PrimOp.readMatFrame(readPayload(), fibo);
        }

        Expr readAdd() {
            return PrimOp.readMatFrame(readPayload(), add);
        }

        Expr readSub() {
            return PrimOp.readMatFrame(readPayload(), sub);
        }

        Expr readLt() {
            return PrimOp.readMatFrame(readPayload(), lt);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }


    // Slightly better than passing by argument (less register pressure?) but still
    // not inlined.
    static class PassMoreFuncInArray extends Expr {
        @Child
        private Expr body;

        PassMoreFuncInArray() {
            Expr isBaseCase = ApplyNode.unknown(readLt(), readN(), PrimOp.lit(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(2));
            Expr ap1 = ApplyNode.unknown(readFibo(), nSub1,
                    readPayload());
            Expr ap2 = ApplyNode.unknown(readFibo(), nSub2,
                    readPayload());
            Expr recurNode = ApplyNode.unknown(readAdd(), ap1, ap2);
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readPayload() {
            return ReadArgNodeGen.create(1);
        }

        Expr readFibo() {
            return PrimOp.readArray(readPayload(), 0);
        }

        Expr readAdd() {
            return PrimOp.readArray(readPayload(), 1);
        }

        Expr readSub() {
            return PrimOp.readArray(readPayload(), 2);
        }

        Expr readLt() {
            return PrimOp.readArray(readPayload(), 3);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }

    // Passing 4 functions as arguments doesn't really work.
    static class PassMoreFunc extends Expr {
        @Child
        private Expr body;

        PassMoreFunc() {
            Expr isBaseCase = PrimOp.callLt(readN(), PrimOp.lit(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.lit(2));
            Expr ap1 = ApplyNode.unknown(readFibo(), nSub1,
                    readFibo(),
                    readAdd(),
                    readSub(),
                    readLt());
            Expr ap2 = ApplyNode.unknown(readFibo(), nSub2,
                    readFibo(),
                    readAdd(),
                    readSub(),
                    readLt());
            Expr recurNode = PrimOp.callAdd(ap1, ap2);
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readFibo() {
            return ReadArgNodeGen.create(1);
        }

        Expr readAdd() {
            return ReadArgNodeGen.create(2);
        }

        Expr readSub() {
            return ReadArgNodeGen.create(3);
        }

        Expr readLt() {
            return ReadArgNodeGen.create(4);
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
