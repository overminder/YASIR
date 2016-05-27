package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.BareFunction;
import com.github.overmind.yasir.value.Box;
import com.github.overmind.yasir.value.Closure;
import com.github.overmind.yasir.value.Nil;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import static com.github.overmind.yasir.Simple.array;

// Playing with different compilation strategies for global functions.
// What works (i.e, fibo(40) on my Sandy Bridge i5-2430M takes ~1 second):
// - inlined primops
// - function wrapped primops as compilation constants
// - wrapped primops as boxed compilation constants,
// - pass wrapped primops in register (only for <= 4 args?)
// - pass wrapped primops in materialized frame (after fixing the ReadNonLocalNode)
// What doesn't:
// - pass too many wrapped primops in registers (4x slow down)
// - pass wrapped primops in array (3x slow down)
public final class TestFiboClosure {
    public static BareFunction create() {
        return createLabeledCPS();
    }

    // Inlined primop nodes and constant closure pointer.
    // 1x speed
    public static BareFunction createFullyTruffled() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot n = fd.addFrameSlot("n", FrameSlotKind.Long);
        RootNode root = RootEntry.create(new FullyTruffled(n, fibo), fd);
        fibo.setTarget(Yasir.rt().createCallTarget(root));
        return fibo;
    }

    // Inlined primop nodes and passing fibo's closure pointer as argument.
    // 1x speed
    public static BareFunction createPassFunc() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new TestFiboClosure.PassFunc());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0), PrimOp.litObj(fibo)));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Inlined primop nodes and passing fibo's closure pointer as boxed argument.
    // 1x speed
    public static BareFunction createPassFuncBoxed() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new TestFiboClosure.PassFuncBoxed());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0), PrimOp.box(PrimOp.litObj(fibo))));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Passing all closure pointers (fibo, primops) as separate arguments.
    // 1/4x speed.
    public static BareFunction createPassMoreFunc() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new PassMoreFunc());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0),
                        PrimOp.litObj(fibo),
                        PrimOp.litObj(PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance())),
                        PrimOp.litObj(PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance())),
                        PrimOp.litObj(PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance()))));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Passing all closure pointers (fibo, primops) as an array argument.
    // 1/3x speed.
    public static BareFunction createPassMoreFuncInArray() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");

        RootNode fiboRoot = RootEntry.create(new PassMoreFuncInArray());
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo, ReadArgNodeGen.create(0),
                        PrimOp.litObj(array(
                                fibo,
                                PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance()),
                                PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance()),
                                PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance())))));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Passing all closure pointers (fibo, primops) from the parent's frame.
    // 5.5x speed after fixing the ReadNonLocalNode. Still not quite good.
    public static BareFunction createPassMoreFuncInParentsMatFrame() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");
        FrameDescriptor trampoFd = new FrameDescriptor();

        FrameSlot fiboSlot = trampoFd.addFrameSlot("fibo");
        FrameSlot addSlot = trampoFd.addFrameSlot("add");
        FrameSlot subSlot = trampoFd.addFrameSlot("sub");
        FrameSlot ltSlot = trampoFd.addFrameSlot("lt");

        RootNode fiboRoot = RootEntry.create(new PassMoreFuncInMatFrame(fiboSlot, addSlot, subSlot, ltSlot));
        RootNode fiboTrampoRoot = RootEntry.create(Begin.create(
                Vars.write(fiboSlot, PrimOp.litObj(fibo)),
                Vars.write(addSlot, PrimOp.litObj(PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance()))),
                Vars.write(subSlot, PrimOp.litObj(PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance()))),
                Vars.write(ltSlot, PrimOp.litObj(PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance()))),
                ApplyNode.known(fibo, ReadArgNodeGen.create(0), PrimOp.matCurrentFrame())
        ), trampoFd);

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Same as above, 5.5x.
    public static BareFunction createPassMoreFuncInFreshlyCreatedMatFrame() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");
        FrameDescriptor trampoFd = new FrameDescriptor();

        FrameSlot fiboSlot = trampoFd.addFrameSlot("fibo");
        FrameSlot addSlot = trampoFd.addFrameSlot("add");
        FrameSlot subSlot = trampoFd.addFrameSlot("sub");
        FrameSlot ltSlot = trampoFd.addFrameSlot("lt");

        RootNode fiboRoot = RootEntry.create
                (new PassMoreFuncInMatFrame(fiboSlot, addSlot, subSlot, ltSlot));
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo,
                        ReadArgNodeGen.create(0),
                        PrimOp.allocMatFrame(
                                array(fiboSlot, addSlot, subSlot, ltSlot),
                                array(PrimOp.litObj(fibo),
                                        PrimOp.litObj(PrimOp.ADD),
                                        PrimOp.litObj(PrimOp.SUB),
                                        PrimOp.litObj(PrimOp.LT)
                                ),
                                trampoFd
                        )
                )
        );

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Also save into local.
    // Same as above, 5.5x.
    public static BareFunction createPassMoreFuncInFreshlyCreatedMatFrameIntoLocal() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");
        FrameDescriptor trampoFd = new FrameDescriptor();

        FrameSlot fiboSlot = trampoFd.addFrameSlot("fibo");
        FrameSlot addSlot = trampoFd.addFrameSlot("add");
        FrameSlot subSlot = trampoFd.addFrameSlot("sub");
        FrameSlot ltSlot = trampoFd.addFrameSlot("lt");

        PassMoreFuncInMatFrameIntoLocal fiboE = new PassMoreFuncInMatFrameIntoLocal(fiboSlot, addSlot, subSlot, ltSlot);
        RootNode fiboRoot = RootEntry.create(fiboE, fiboE.fd);
        RootNode fiboTrampoRoot = RootEntry.create(
                ApplyNode.known(fibo,
                        ReadArgNodeGen.create(0),
                        PrimOp.allocMatFrame(
                                array(fiboSlot, addSlot, subSlot, ltSlot),
                                array(PrimOp.litObj(fibo),
                                        PrimOp.litObj(PrimOp.ADD),
                                        PrimOp.litObj(PrimOp.SUB),
                                        PrimOp.litObj(PrimOp.LT)
                                ),
                                trampoFd
                        )
                )
        );

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // Inline all closure pointers (fibo, primops) in the code.
    // 1x speed.
    // This is what SL uses. And yes, this is among the fastest implementations.
    public static BareFunction createInjectClosuresThroughCompilationContext() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");

        BareFunction add = PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance());
        BareFunction sub = PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance());
        BareFunction lt = PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance());

        RootNode fiboRoot = RootEntry.create(
                new InjectClosureThroughCompilationContext(fibo, add, sub, lt));
        RootNode fiboTrampoRoot = RootEntry.create(ApplyNode.known(fibo, ReadArgNodeGen.create(0)));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }


    // Inline all closure pointers (fibo, primops) boxed in the code.
    // 1x speed.
    public static BareFunction createInjectBoxedClosuresThroughCompilationContext() {
        BareFunction fibo = new BareFunction(null, "fibo-closure");
        BareFunction fiboTrampo = new BareFunction(null, "fibo-trampo");

        BareFunction add = PrimOp.makeBinaryClosure(PrimOpFactory.AddFactory.getInstance());
        BareFunction sub = PrimOp.makeBinaryClosure(PrimOpFactory.SubFactory.getInstance());
        BareFunction lt = PrimOp.makeBinaryClosure(PrimOpFactory.LtFactory.getInstance());

        RootNode fiboRoot = RootEntry.create(
                new InjectClosureThroughCompilationContext(fibo, add, sub, lt));
        RootNode fiboTrampoRoot = RootEntry.create(ApplyNode.known(fibo, ReadArgNodeGen.create(0)));

        fibo.setTarget(Yasir.rt().createCallTarget(fiboRoot));
        fiboTrampo.setTarget(Yasir.rt().createCallTarget(fiboTrampoRoot));
        return fiboTrampo;
    }

    // inline primops with only the fibo function CPS-ed.
    // 100x slow down.
    static BareFunction createFastCPSWithArrayBasedClosure() {
        BareFunction fiboEntryB = BareFunction.empty("fibo-entry"),
                fiboRet1C = BareFunction.empty("fibo-ret1"),
                fiboRet2C = BareFunction.empty("fibo-ret2");
        Closure fiboEntryC = fiboEntryB.withPayloads(array());
        Expr fiboEntry, fiboRet1, fiboRet2;
        {
            fiboEntry = new Expr() {
                Expr isBaseCase = PrimOp.lt(readN(), PrimOp.litL(2));
                Expr nSub1 = PrimOp.sub(readN(), PrimOp.litL(1));
                Expr mkK = Closures.alloc(fiboRet1C, readN(), readK());
                Expr recurNode = ApplyNode.unknownWithPayload(PrimOp.litObj(fiboEntryC), nSub1, mkK);
                @Child Expr body = new IfNode(isBaseCase,
                        ApplyNode.unknownWithPayload(readK(), readN()),
                        recurNode);

                private Expr readN() {
                    return Vars.read(1);
                }

                private Expr readK() {
                    return Vars.read(2);
                }

                @Override
                public Object executeGeneric(VirtualFrame frame) {
                    return body.executeGeneric(frame);
                }
            };
        }

        {
            fiboRet1 = new Expr() {
                Expr nSub2 = PrimOp.sub(readN(), PrimOp.litL(2));
                Expr mkK = Closures.alloc(fiboRet2C, readRes(), readK());
                Expr recurNode = ApplyNode.unknownWithPayload(PrimOp.litObj(fiboEntryC), nSub2, mkK);
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

        fiboEntryB.setTarget(Yasir.rt().createCallTarget(RootEntry.create(fiboEntry)));
        fiboRet1C.setTarget(Yasir.rt().createCallTarget(RootEntry.create(fiboRet1)));
        fiboRet2C.setTarget(Yasir.rt().createCallTarget(RootEntry.create(fiboRet2)));

        BareFunction idB = new BareFunction(Yasir.rt().createCallTarget(RootEntry.create(Vars.read(1))), "id");
        Closure idC = idB.withPayloads(array());
        BareFunction fibo = new BareFunction(Yasir.rt().createCallTarget(RootEntry.create(
                ApplyNode.known(fiboEntryB, PrimOp.litObj(fiboEntryB), Vars.read(0), PrimOp.litObj(idC)))),
                "fibo");

        return fibo;
    }

    // Use a switch to dispatch labels.
    static BareFunction createLabeledCPS() {
        BareFunction fibo = BareFunction.empty("fibo");
        FrameDescriptor fd = new FrameDescriptor();

        FrameSlot n = fd.addFrameSlot("n");  // first arg and ret val
        FrameSlot t = fd.addFrameSlot("t");  // saved n
        FrameSlot cont = fd.addFrameSlot("cont");
        FrameSlot env = fd.addFrameSlot("env");

        final long FIBO_ENTRY = 0,
                CALL_RET1 = 1,
                CALL_RET2 = 2,
                HALT = 3;

        fibo.setTarget(Yasir.rt().createCallTarget(RootEntry.create(new Expr() {
            @Child Expr populateFrame = Begin.create(
                    Vars.write(n, Vars.read(0)),
                    Vars.write(cont, PrimOp.litL(FIBO_ENTRY)),
                    Vars.write(env, PrimOp.litObj(array(HALT, Nil.INSTANCE, Nil.INSTANCE)))
            );

            @Child LoopNode loopNode = Yasir.rt().createLoopNode(new TestLoopClosure.FastRepNode(
                    PrimOp.longNeq(Vars.read(cont), PrimOp.litL(HALT)),
                    new Expr() {
                        @Child
                        Expr readCont = Vars.read(cont);

                        @Child
                        Expr fiboEntry = new IfNode(
                                PrimOp.lt(Vars.read(n), PrimOp.litL(2)),
                                runCont(Vars.read(n)),
                                allocContAndJump(CALL_RET1, Vars.read(env), Vars.read(n),
                                        FIBO_ENTRY, PrimOp.sub(Vars.read(n), PrimOp.litL(1)))
                        );

                        @Child
                        Expr fiboRet1 = allocContAndJump(CALL_RET2, Vars.read(env), Vars.read(n),
                                FIBO_ENTRY, PrimOp.sub(Vars.read(t), PrimOp.litL(2)));

                        @Child
                        Expr fiboRet2 = runCont(PrimOp.add(Vars.read(n), Vars.read(t)));

                        private Expr allocContAndJump(long contV, Expr readEnv, Expr readN, long jumpTo, Expr withArg) {
                            return new Expr() {
                                @Child
                                Expr pushCont = Vars.write(env, PrimOp.allocArray(PrimOp.litL(contV), readEnv, readN));
                                @Child
                                Expr writeArg = Vars.write(n, withArg);
                                @Child
                                Expr jump = Vars.write(cont, PrimOp.litL(jumpTo));

                                @Override
                                public Object executeGeneric(VirtualFrame frame) {
                                    pushCont.executeGeneric(frame);
                                    writeArg.executeGeneric(frame);
                                    jump.executeGeneric(frame);
                                    return Nil.INSTANCE;
                                }
                            };
                        }

                        Expr runCont(Expr retVal) {
                            return new Expr() {
                                @Child
                                Expr writeRetVal = Vars.write(n, retVal);
                                @Child
                                Expr popCont = Vars.write(cont, PrimOp.readArray(Vars.read(env), 0));
                                @Child
                                Expr popEnv = Vars.write(env, PrimOp.readArray(Vars.read(env), 1));
                                @Child
                                Expr popSavedN = Vars.write(t, PrimOp.readArray(Vars.read(env), 2));

                                @Override
                                public Object executeGeneric(VirtualFrame frame) {
                                    writeRetVal.executeGeneric(frame);
                                    popCont.executeGeneric(frame);
                                    popSavedN.executeGeneric(frame);
                                    popEnv.executeGeneric(frame);
                                    return Nil.INSTANCE;
                                }
                            };
                        }

                        @Override
                        public Object executeGeneric(VirtualFrame frame) {
                            try {
                                long lbl = readCont.executeLong(frame);
                                switch ((int) lbl) {
                                    case (int) FIBO_ENTRY:
                                        fiboEntry.executeGeneric(frame);
                                        break;
                                    case (int) CALL_RET1:
                                        fiboRet1.executeGeneric(frame);
                                        break;
                                    case (int) CALL_RET2:
                                        fiboRet2.executeGeneric(frame);
                                        break;
                                    default:
                                        throw new RuntimeException("Unepected label: " + lbl);
                                }
                            } catch (UnexpectedResultException e) {
                                throw new RuntimeException(e);
                            }

                            return Nil.INSTANCE;
                        }
                    }
            ));

            @Child Expr readN = Vars.read(n);

            @Override
            public Object executeGeneric(VirtualFrame frame) {
                populateFrame.executeGeneric(frame);
                loopNode.executeLoop(frame);
                return readN.executeGeneric(frame);
            }
        }, fd)));

        return fibo;
    }

    static class InjectBoxedClosureThroughCompilationContext extends Expr {
        @Child
        private Expr body;

        Box fibo;
        Box add;
        Box sub;
        Box lt;

        InjectBoxedClosureThroughCompilationContext(BareFunction fibo, BareFunction add, BareFunction sub, BareFunction lt) {
            this.fibo = new Box(fibo);
            this.add = new Box(add);
            this.sub = new Box(sub);
            this.lt = new Box(lt);

            Expr isBaseCase = ApplyNode.unknown(readLt(), readN(), PrimOp.litL(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(2));
            Expr ap1 = ApplyNode.unknown(readFibo(), nSub1);
            Expr ap2 = ApplyNode.unknown(readFibo(), nSub2);
            Expr recurNode = ApplyNode.unknown(readAdd(), ap1, ap2);
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readFibo() {
            return PrimOp.readBox(PrimOp.litObj(fibo));
        }

        Expr readAdd() {
            return PrimOp.readBox(PrimOp.litObj(add));
        }

        Expr readSub() {
            return PrimOp.readBox(PrimOp.litObj(sub));
        }

        Expr readLt() {
            return PrimOp.readBox(PrimOp.litObj(lt));
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }

    static class InjectClosureThroughCompilationContext extends Expr {
        @Child
        private Expr body;

        BareFunction fibo;
        BareFunction add;
        BareFunction sub;
        BareFunction lt;

        InjectClosureThroughCompilationContext(BareFunction fibo, BareFunction add, BareFunction sub, BareFunction lt) {
            this.fibo = fibo;
            this.add = add;
            this.sub = sub;
            this.lt = lt;

            Expr isBaseCase = ApplyNode.unknown(readLt(), readN(), PrimOp.litL(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(2));
            Expr ap1 = ApplyNode.unknown(readFibo(), nSub1);
            Expr ap2 = ApplyNode.unknown(readFibo(), nSub2);
            Expr recurNode = ApplyNode.unknown(readAdd(), ap1, ap2);
            body = new IfNode(isBaseCase, readN(), recurNode);
        }

        Expr readN() {
            return ReadArgNodeGen.create(0);
        }

        Expr readFibo() {
            return PrimOp.litObj(fibo);
        }

        Expr readAdd() {
            return PrimOp.litObj(add);
        }

        Expr readSub() {
            return PrimOp.litObj(sub);
        }

        Expr readLt() {
            return PrimOp.litObj(lt);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }

    static class PassMoreFuncInMatFrameIntoLocal extends Expr {
        private final FrameSlot fiboL;
        private final FrameSlot addL;
        private final FrameSlot subL;
        private final FrameSlot ltL;

        @Child
        private Expr body;

        FrameSlot fibo;
        FrameSlot add;
        FrameSlot sub;
        FrameSlot lt;

        final FrameDescriptor fd = new FrameDescriptor();

        PassMoreFuncInMatFrameIntoLocal(FrameSlot fibo, FrameSlot add, FrameSlot sub, FrameSlot lt) {
            this.fibo = fibo;
            this.add = add;
            this.sub = sub;
            this.lt = lt;

            this.fiboL = fd.addFrameSlot("fibo");
            this.addL = fd.addFrameSlot("add");
            this.subL = fd.addFrameSlot("sub");
            this.ltL = fd.addFrameSlot("lt");

            Expr assignArgs = Begin.create(
                    Vars.write(fiboL, readFibo()),
                    Vars.write(addL, readAdd()),
                    Vars.write(subL, readSub()),
                    Vars.write(ltL, readLt())
            );

            Expr isBaseCase = ApplyNode.unknown(readLtL(), readN(), PrimOp.litL(2));
            Expr nSub1 = ApplyNode.unknown(readSubL(), readN(), PrimOp.litL(1));
            Expr nSub2 = ApplyNode.unknown(readSubL(), readN(), PrimOp.litL(2));
            Expr ap1 = ApplyNode.unknown(readFiboL(), nSub1,
                    readPayload());
            Expr ap2 = ApplyNode.unknown(readFiboL(), nSub2,
                    readPayload());
            Expr recurNode = ApplyNode.unknown(readAddL(), ap1, ap2);
            body = Begin.create(assignArgs, new IfNode(isBaseCase, readN(), recurNode));
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

        Expr readFiboL() {
            return Vars.read(fiboL);
        }

        Expr readAdd() {
            return PrimOp.readMatFrame(readPayload(), add);
        }

        Expr readAddL() {
            return Vars.read(addL);
        }

        Expr readSub() {
            return PrimOp.readMatFrame(readPayload(), sub);
        }

        Expr readSubL() {
            return Vars.read(subL);
        }

        Expr readLt() {
            return PrimOp.readMatFrame(readPayload(), lt);
        }

        Expr readLtL() {
            return Vars.read(ltL);
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

            Expr isBaseCase = ApplyNode.unknown(readLt(), readN(), PrimOp.litL(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(2));
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
            Expr isBaseCase = ApplyNode.unknown(readLt(), readN(), PrimOp.litL(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(2));
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
            Expr isBaseCase = PrimOp.callLt(readN(), PrimOp.litL(2));
            Expr nSub1 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(1));
            Expr nSub2 = ApplyNode.unknown(readSub(), readN(), PrimOp.litL(2));
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
            Expr isBaseCase = PrimOp.lt(readN(), PrimOp.litL(2));
            Expr recurNode = PrimOp.add(
                    ApplyNode.unknown(readFiboUnbox(), PrimOp.sub(readN(), PrimOp.litL(1)), readFibo()),
                    ApplyNode.unknown(readFiboUnbox(), PrimOp.sub(readN(), PrimOp.litL(2)), readFibo()));
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
            Expr isBaseCase = PrimOp.lt(readN(), PrimOp.litL(2));
            Expr recurNode = PrimOp.add(
                    ApplyNode.unknown(readFibo(), PrimOp.sub(readN(), PrimOp.litL(1)), readFibo()),
                    ApplyNode.unknown(readFibo(), PrimOp.sub(readN(), PrimOp.litL(2)), readFibo()));
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

        FullyTruffled(FrameSlot n, BareFunction fibo) {
            this.n = n;

            populateFrame = WriteLocalNodeGen.create(readArg(), n);
            Expr isBaseCase = PrimOp.lt(readLocal(), PrimOp.litL(2));
            Expr recurNode = PrimOp.add(
                    ApplyNode.unknown(PrimOp.litObj(fibo), PrimOp.sub(readLocal(), PrimOp.litL(1))),
                    ApplyNode.unknown(PrimOp.litObj(fibo), PrimOp.sub(readLocal(), PrimOp.litL(2))));
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
        private final BareFunction fibo;
        @Child
        private WriteLocalNode populateFrame;
        @Child
        private Expr readArg;
        @Child
        private Expr readLocal;
        @Child
        private Expr lit1Node = PrimOp.litL(1);
        @Child
        private Expr lit2Node = PrimOp.litL(2);
        // @Child private Expr body;
        @Child
        private Expr isBaseCase;
        @Child
        private Expr recurNode;

        Fast(FrameSlot n, BareFunction fibo) {
            this.n = n;
            this.fibo = fibo;
            Expr loadFibo = PrimOp.litObj(fibo);
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
            return (Long) readArg.executeGeneric(frame);
        }

        long readN(VirtualFrame frame) {
            return (Long) readLocal.executeGeneric(frame);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            // frame.setLong(n, readN(frame));
            populateFrame.executeGeneric(frame);

            if ((Boolean) isBaseCase.executeGeneric(frame)) {
                return readN(frame);
            } else {
                return recurNode.executeGeneric(frame);
            }
        }
    }
}
