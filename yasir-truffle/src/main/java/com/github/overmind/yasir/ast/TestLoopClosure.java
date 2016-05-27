package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.BareFunction;
import com.github.overmind.yasir.value.Nil;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

import static com.github.overmind.yasir.Simple.array;

// Loops and tail calls.
public final class TestLoopClosure {
    public static BareFunction create() {
        return createCheckWhyIAmSlow();
    }

    public static long call(long n) {
        return (Long) Yasir.createCallTarget(ApplyNode.known(create(), PrimOp.litL(n))).call();
    }

    // Slow because the argument allocation can't be removed. Using an exception is even 1.5x slower!
    private static BareFunction createCheckWhyIAmSlow() {
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot cont = fd.addFrameSlot("cont");
        FrameSlot args = fd.addFrameSlot("args");
        FrameSlot resS = fd.addFrameSlot("res");

        // FrameSlot n = fd.addFrameSlot("n");
        // FrameSlot i = fd.addFrameSlot("i", FrameSlotKind.Object);

        BareFunction trampo = BareFunction.empty("loop-trampo");
        BareFunction loop = BareFunction.empty("loop");   // [0]: i, [1]: s
        BareFunction id = new BareFunction(Yasir.createCallTarget(Vars.read(0)), "id");

        trampo.setTarget(Yasir.createCallTarget(new Expr() {
            // @Child private Expr populateFrame = Vars.write(n, Vars.read(0));

            @Child private LoopNode loopNode = Yasir.rt().createLoopNode(new FastRepNode(new Expr() {
                @Child private Expr getCont = Vars.read(cont);
                @Child private Expr getArgs = Vars.read(args);
                @Child private DispatchClosureNode dispatch = DispatchClosureNodeGen.create();

                @Override
                public Object executeGeneric(VirtualFrame frame) {
                    try {
                        BareFunction func = getCont.executeBareFunction(frame);
                        if (func == id) {
                            return false;
                        } else {
                            Object[] res = (Object[]) dispatch.executeDispatch(frame, func, (Object[]) getArgs.executeGeneric(frame));
                            BareFunction contValue = (BareFunction) res[0];
                            if (contValue == id) {
                                frame.setObject(resS, ((Object[]) res[1])[0]);
                                return false;
                            }
                            frame.setObject(cont, res[0]);
                            frame.setObject(args, res[1]);
                            return true;
                        }
                    } catch (UnexpectedResultException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
                    Begin.create()
            ));

            // @Child private Expr readRes = Vars.read(n);

            @Override
            public Object executeGeneric(VirtualFrame frame) {
                long n = (long) frame.getArguments()[0];
                frame.setObject(cont, loop);
                frame.setObject(args, array(n, 0L, id));
                // BareFunction contValue = loop;
                // Object[] argValues = (Object[]) array(n, 0L, id);

                loopNode.executeLoop(frame);
                // return readRes.executeGeneric(frame);
                return frame.getValue(resS);
            }
        }, fd));

        FrameDescriptor innerFd = new FrameDescriptor();
        FrameSlot innerI = innerFd.addFrameSlot("i");

        loop.setTarget(Yasir.createCallTarget(new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                long i = (long) args[0];
                long s = (long) args[1];
                Object k = args[2];
                if (i < 1) {
                    return array(k, array(s));
                } else {
                    return array(loop, array(i - 1, s + i, k));
                }
            }
        }));

        return trampo;
    }

    // Too many allocations. This is only slightly better than the explicit tailcall
    private static BareFunction createTailcallWithExplicitCPSLoop() {
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot cont = fd.addFrameSlot("cont", FrameSlotKind.Object);
        FrameSlot args = fd.addFrameSlot("args", FrameSlotKind.Object);

        BareFunction trampo = BareFunction.empty("loop-trampo");
        BareFunction loop = BareFunction.empty("loop");   // [0]: i, [1]: s
        BareFunction id = new BareFunction(Yasir.createCallTarget(Vars.read(0)), "id");

        trampo.setTarget(Yasir.createCallTarget(new Expr() {
            @Child private DispatchClosureNode dispatch = DispatchClosureNodeGen.create();
            // @Child private Expr getCont = Vars.read(cont);
            // @Child private Expr getArgs = Vars.read(args);

            @Override
            public Object executeGeneric(VirtualFrame frame) {
                // frame.setObject(cont, loop);
                long n = (Long) frame.getArguments()[0];
                // frame.setObject(args, array(n, 0L, id));
                BareFunction contValue = loop;
                Object[] argValues = (Object[]) array(n, 0L, id);

                while (true) {
                    Object res = dispatch.executeDispatch(frame, contValue, argValues);
                    if (res instanceof Object[]) {
                        Object[] asArr = (Object[]) res;
                        // frame.setObject(cont, asArr[0]);
                        // frame.setObject(args, asArr[1]);
                        contValue = (BareFunction) asArr[0];
                        argValues = (Object[]) asArr[1];
                    } else {
                        return res;
                    }
                }
            }
        }, fd));

        loop.setTarget(Yasir.createCallTarget(new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                Object[] loopArgs = frame.getArguments();
                long i = (Long) loopArgs[0];
                long s = (Long) loopArgs[1];
                BareFunction k = (BareFunction) loopArgs[2];
                if (i < 1) {
                    return array(k, array(s));
                } else {
                    return array(loop, array(i - 1, s + i, k));
                }
            }
        }));

        return trampo;
    }

    // Same as passing the state in the arguments.
    private static BareFunction createTailcallMutatingMatFrame() {
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        BareFunction trampo = BareFunction.empty("loop-trampo");
        BareFunction loop = BareFunction.empty("loop");   // [0]: i, [1]: s

        // Accessing the parent's frame through TruffleRuntime.getParentFrame cause a
        // 'too deep inlining' error. Not sure why...
        loop.setTarget(Yasir.createCallTarget(new IfNode(
                PrimOp.lt(PrimOp.readMatFrame(Vars.read(0), i), PrimOp.litL(1)),
                PrimOp.readMatFrame(Vars.read(0), s),
                Begin.create(
                        PrimOp.writeMatFrame(Vars.read(0), s,
                                PrimOp.add(PrimOp.readMatFrame(Vars.read(0), s), PrimOp.readMatFrame(Vars.read(0), i))),
                        PrimOp.writeMatFrame(Vars.read(0), i,
                                PrimOp.sub(PrimOp.readMatFrame(Vars.read(0), i), PrimOp.litL(1))),
                        ApplyNode.knownTail(loop, Vars.read(0))
                )
        )));

        trampo.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                ApplyNode.known(loop, PrimOp.matCurrentFrame())
        ), fd));
        return trampo;
    }

    // Very bad... at least 100x slowdown.
    // Quote from http://markmail.org/message/ehou7ansc6dicllg#query:+page:1+mid:a6ruspukl4i4vgmq+state:results
    // "That said, if Truffle does not see the exception thrown and caught in the
    // same compilation scope there is some price to pay for tail calls atm, yes."
    private static BareFunction createTailcallWithStateInArgs() {
        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        BareFunction trampo = BareFunction.empty("loop-trampo");
        BareFunction loop = BareFunction.empty("loop");   // [0]: i, [1]: s
        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, Vars.read(1)),
                new IfNode(
                        PrimOp.lt(Vars.read(i), PrimOp.litL(1)),
                        Vars.read(s),
                        ApplyNode.knownTail(loop,
                                PrimOp.sub(Vars.read(i), PrimOp.litL(1)),
                                PrimOp.add(Vars.read(s), Vars.read(i)))
                )
        ), fd));
        trampo.setTarget(Yasir.createCallTarget(ApplyNode.known(loop,
                Vars.read(0),
                PrimOp.litL(0))));
        return trampo;
    }

    // 1x. Storing the functions into local variables works.
    private static BareFunction createPassingPrimOpsInFreshMatFrameIntoLocal() {
        BareFunction trampo = BareFunction.empty("loop-pass-primops-trampo");
        BareFunction loop = BareFunction.empty("loop-pass-primops");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        FrameSlot addL = fd.addFrameSlot("add");
        FrameSlot subL = fd.addFrameSlot("sub");
        FrameSlot ltL = fd.addFrameSlot("lt");

        FrameDescriptor trampoFd = new FrameDescriptor();
        FrameSlot add = trampoFd.addFrameSlot("add");
        FrameSlot sub = trampoFd.addFrameSlot("sub");
        FrameSlot lt = trampoFd.addFrameSlot("lt");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                Vars.write(addL, PrimOp.readMatFrame(Vars.read(1), add)),
                Vars.write(subL, PrimOp.readMatFrame(Vars.read(1), sub)),
                Vars.write(ltL, PrimOp.readMatFrame(Vars.read(1), lt)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(Vars.read(ltL),
                                            PrimOp.litL(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    Vars.read(addL),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    Vars.read(subL),
                                                    Vars.read(i), PrimOp.litL(1)))
                                    )
                            )
                    );

                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return Nil.INSTANCE;
                    }
                },
                Vars.read(s)
        ), fd));

        trampo.setTarget(Yasir.createCallTarget(
                ApplyNode.known(loop,
                        Vars.read(0),
                        PrimOp.allocMatFrame(
                                array(add, sub, lt),
                                array(PrimOp.litObj(PrimOp.ADD), PrimOp.litObj(PrimOp.SUB), PrimOp.litObj(PrimOp.LT)),
                                trampoFd
                        )
                )
        ));
        return trampo;
    }

    // Same as using the parent's mat frame (4x slow down). Not quite good.
    private static BareFunction createPassingPrimOpsInFreshMatFrame() {
        BareFunction trampo = BareFunction.empty("loop-pass-primops-trampo");
        BareFunction loop = BareFunction.empty("loop-pass-primops");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        FrameSlot arr = fd.addFrameSlot("arr");

        FrameDescriptor trampoFd = new FrameDescriptor();
        FrameSlot add = trampoFd.addFrameSlot("add");
        FrameSlot sub = trampoFd.addFrameSlot("sub");
        FrameSlot lt = trampoFd.addFrameSlot("lt");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                Vars.write(arr, Vars.read(1)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(PrimOp.readMatFrame(Vars.read(arr), lt),
                                            PrimOp.litL(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), add),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), sub),
                                                    Vars.read(i), PrimOp.litL(1)))
                                    )
                            )
                    );

                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return Nil.INSTANCE;
                    }
                },
                Vars.read(s)
        ), fd));

        trampo.setTarget(Yasir.createCallTarget(
                ApplyNode.known(loop,
                        Vars.read(0),
                        PrimOp.allocMatFrame(
                                array(add, sub, lt),
                                array(PrimOp.litObj(PrimOp.ADD), PrimOp.litObj(PrimOp.SUB), PrimOp.litObj(PrimOp.LT)),
                                trampoFd
                        )
                )
        ));
        return trampo;
    }

    // Primops function passed in an mat frame.
    // This incurs some (~4x) slow downs, but might be still acceptable. Note sure why
    // this can't be optimized...
    private static BareFunction createReadingPrimOpsFromParentsMatFrame() {
        BareFunction trampo = BareFunction.empty("loop-pass-primops-trampo");
        BareFunction loop = BareFunction.empty("loop-pass-primops");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        FrameSlot arr = fd.addFrameSlot("arr");

        FrameDescriptor trampoFd = new FrameDescriptor();
        FrameSlot add = trampoFd.addFrameSlot("add");
        FrameSlot sub = trampoFd.addFrameSlot("sub");
        FrameSlot lt = trampoFd.addFrameSlot("lt");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                Vars.write(arr, new Expr() {
                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        return Yasir.rt().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_ONLY, false);
                    }
                }),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(PrimOp.readMatFrame(Vars.read(arr), lt),
                                            PrimOp.litL(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), add),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), sub),
                                                    Vars.read(i), PrimOp.litL(1)))
                                    )
                            )
                    );

                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return Nil.INSTANCE;
                    }
                },
                Vars.read(s)
        ), fd));

        trampo.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(add, PrimOp.litObj(PrimOp.ADD)),
                Vars.write(sub, PrimOp.litObj(PrimOp.SUB)),
                Vars.write(lt, PrimOp.litObj(PrimOp.LT)),
                ApplyNode.known(loop, Vars.read(0))
        ), trampoFd));
        return trampo;
    }

    // Primops function passed in an mat frame.
    // This incurs some (~4x) slow downs, but might be still acceptable. Note sure why
    // this can't be optimized...
    private static BareFunction createPassingPrimOpsInParentsMatFrame() {
        BareFunction trampo = BareFunction.empty("loop-pass-primops-trampo");
        BareFunction loop = BareFunction.empty("loop-pass-primops");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        FrameSlot arr = fd.addFrameSlot("arr");

        FrameDescriptor trampoFd = new FrameDescriptor();
        FrameSlot add = trampoFd.addFrameSlot("add");
        FrameSlot sub = trampoFd.addFrameSlot("sub");
        FrameSlot lt = trampoFd.addFrameSlot("lt");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                Vars.write(arr, Vars.read(1)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(PrimOp.readMatFrame(Vars.read(arr), lt),
                                            PrimOp.litL(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), add),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), sub),
                                                    Vars.read(i), PrimOp.litL(1)))
                                    )
                            )
                    );

                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return Nil.INSTANCE;
                    }
                },
                Vars.read(s)
        ), fd));

        trampo.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(add, PrimOp.litObj(PrimOp.ADD)),
                Vars.write(sub, PrimOp.litObj(PrimOp.SUB)),
                Vars.write(lt, PrimOp.litObj(PrimOp.LT)),
                ApplyNode.known(loop,
                        Vars.read(0),
                        PrimOp.matCurrentFrame())
        ), trampoFd));
        return trampo;
    }

    // Primops function passed in an array arg. Also works - Graal's loop-based optimizations
    // are truly great.
    private static BareFunction createPassingPrimOpsInArray() {
        BareFunction trampo = BareFunction.empty("loop-pass-primops-trampo");
        BareFunction loop = BareFunction.empty("loop-pass-primops");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        FrameSlot arr = fd.addFrameSlot("arr");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                Vars.write(arr, Vars.read(1)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(PrimOp.readArray(Vars.read(arr), 2),
                                            PrimOp.litL(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    PrimOp.readArray(Vars.read(arr), 0),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    PrimOp.readArray(Vars.read(arr), 1),
                                                    Vars.read(i), PrimOp.litL(1)))
                                    )
                            )
                    );

                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return Nil.INSTANCE;
                    }
                },
                Vars.read(s)
        ), fd));

        trampo.setTarget(Yasir.createCallTarget(
                ApplyNode.known(loop,
                        Vars.read(0),
                        PrimOp.litObj(array(PrimOp.ADD, PrimOp.SUB, PrimOp.LT)))));
        return trampo;
    }

    // Primops function passed in args. Works well.
    private static BareFunction createPassingPrimOps() {
        BareFunction trampo = BareFunction.empty("loop-pass-primops-trampo");
        BareFunction loop = BareFunction.empty("loop-pass-primops");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        FrameSlot add = fd.addFrameSlot("add");
        FrameSlot sub = fd.addFrameSlot("sub");
        FrameSlot lt = fd.addFrameSlot("lt");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                Vars.write(add, Vars.read(1)),
                Vars.write(sub, Vars.read(2)),
                Vars.write(lt, Vars.read(3)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(Vars.read(lt), PrimOp.litL(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    Vars.read(add), Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    Vars.read(sub), Vars.read(i), PrimOp.litL(1)))
                                    )
                            )
                    );

                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return Nil.INSTANCE;
                    }
                },
                Vars.read(s)
        ), fd));

        trampo.setTarget(Yasir.createCallTarget(
                ApplyNode.known(loop,
                        Vars.read(0),
                        PrimOp.litObj(PrimOp.ADD),
                        PrimOp.litObj(PrimOp.SUB),
                        PrimOp.litObj(PrimOp.LT))));
        return trampo;
    }

    // With boxed long, repeatingNode and primop and var read/write nodes.
    private static BareFunction createFaster() {
        BareFunction loop = BareFunction.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");

        class LoopBody extends Node implements RepeatingNode {
            @Child Expr check = PrimOp.lt(PrimOp.litL(0L), Vars.read(i));
            Expr addSI = Vars.write(s, PrimOp.add(Vars.read(s), Vars.read(i)));
            Expr subI1 = Vars.write(i, PrimOp.sub(Vars.read(i), PrimOp.litL(1)));
            @Child Expr body = Begin.create(addSI, subI1);

            @Override
            public boolean executeRepeating(VirtualFrame frame) {
                if ((Boolean) check.executeGeneric(frame)) {
                    body.executeGeneric(frame);
                    return true;
                } else {
                    return false;
                }
            }
        }

        Expr simpleReadS = new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                return frame.getValue(s);
            }
        };

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0L)),
                new Expr() {
                    @Child private LoopNode loopBody = Yasir.rt().createLoopNode(new LoopBody());
                    @Child private Expr result = Vars.read(s);
                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return result.executeGeneric(frame);
                    }
        }), fd));

        return loop;
    }

    // With boxed long and repeating node. Truffle is able to optimize those out.
    private static BareFunction createFastestBoxed() {
        BareFunction loop = BareFunction.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i", FrameSlotKind.Object);
        FrameSlot s = fd.addFrameSlot("s", FrameSlotKind.Object);

        class LoopBody extends Node implements RepeatingNode {
            @Override
            public boolean executeRepeating(VirtualFrame frame) {
                try {
                    if ((Long) frame.getObject(i) > 0) {
                        frame.setObject(s, (Long) frame.getObject(i) + (Long) frame.getObject(s));
                        frame.setObject(i, (Long) frame.getObject(i) - 1L);
                        return true;
                    } else {
                        return false;
                    }
                } catch (FrameSlotTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        loop.setTarget(Yasir.createCallTarget(new Expr() {
            @Child private LoopNode loopBody = Yasir.rt().createLoopNode(new LoopBody());
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                frame.setObject(i, frame.getArguments()[0]);
                frame.setObject(s, 0L);
                loopBody.executeLoop(frame);
                return frame.getValue(s);
            }
        }, fd));

        return loop;
    }


    // With unboxed long and repeating node.
    private static BareFunction createFastest() {
        BareFunction loop = BareFunction.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i", FrameSlotKind.Long);
        FrameSlot s = fd.addFrameSlot("s", FrameSlotKind.Long);

        class LoopBody extends Node implements RepeatingNode {
            @Override
            public boolean executeRepeating(VirtualFrame frame) {
                try {
                    if (frame.getLong(i) > 0L) {
                        frame.setLong(s, frame.getLong(i) + frame.getLong(s));
                        frame.setLong(i, frame.getLong(i) - 1L);
                        return true;
                    } else {
                        return false;
                    }
                } catch (FrameSlotTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        loop.setTarget(Yasir.createCallTarget(new Expr() {
            @Child private LoopNode loopBody = Yasir.rt().createLoopNode(new LoopBody());
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                frame.setLong(i, (Long) frame.getArguments()[0]);
                frame.setLong(s, 0L);
                loopBody.executeLoop(frame);
                try {
                    return frame.getLong(s);
                } catch (FrameSlotTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        }, fd));

        return loop;
    }

    // Done in the standard Truffle way: inlined primops and repeating node.
    private static BareFunction createStandard() {
        BareFunction trampo = BareFunction.empty("loop-fast-trampo");

        // [0]: closure ptr, [1]: i, [2]: s
        BareFunction loop = BareFunction.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    PrimOp.lt(PrimOp.litL(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, PrimOp.add(Vars.read(s), Vars.read(i))),
                                            Vars.write(i, PrimOp.sub(Vars.read(i), PrimOp.litL(1)))
                                    )
                            )
                    );

                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return Nil.INSTANCE;
                    }
                },
                Vars.read(s)
        ), fd));

        trampo.setTarget(Yasir.createCallTarget(
                ApplyNode.known(loop, Vars.read(0))));
        return trampo;
    }

    // Inlined primops and boxed frameslot. Truffle can't optimize the boxings out in this case.
    // 10x for only one slot boxed and 20x slowdown for both.
    private static BareFunction createStandardBoxed() {
        BareFunction trampo = BareFunction.empty("loop-fast-trampo");

        // [0]: closure ptr, [1]: i, [2]: s
        BareFunction loop = BareFunction.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i", FrameSlotKind.Object);
        FrameSlot s = fd.addFrameSlot("s", FrameSlotKind.Object);

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.litL(0)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    PrimOp.lt(PrimOp.litL(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, PrimOp.add(Vars.read(s), Vars.read(i))),
                                            Vars.write(i, PrimOp.sub(Vars.read(i), PrimOp.litL(1)))
                                    )
                            )
                    );

                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        loopBody.executeLoop(frame);
                        return Nil.INSTANCE;
                    }
                },
                Vars.read(s)
        ), fd));

        trampo.setTarget(Yasir.createCallTarget(
                ApplyNode.known(loop, Vars.read(0))));
        return trampo;
    }

    static final class FastRepNode extends Node implements RepeatingNode {
        @Child
        private Expr check;

        @Child
        private Expr body;

        private final ConditionProfile profile = ConditionProfile.createCountingProfile();

        public FastRepNode(Expr check, Expr body) {
            this.check = check;
            this.body = body;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            try {
                if (profile.profile(check.executeBoolean(frame))) {
                    body.executeGeneric(frame);
                    return true;
                }
                return false;
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
