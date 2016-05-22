package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.Yasir;
import com.github.overmind.yasir.value.Closure;
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
    public static Closure create() {
        return createPassingPrimOpsInFreshMatFrameIntoLocal();
    }

    public static long call(long n) {
        return (Long) Yasir.createCallTarget(ApplyNode.known(create(), PrimOp.lit(n))).call();
    }

    // 1x. Storing the functions into local variables works.
    private static Closure createPassingPrimOpsInFreshMatFrameIntoLocal() {
        Closure trampo = Closure.empty("loop-pass-primops-trampo");
        Closure loop = Closure.empty("loop-pass-primops");

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
                Vars.write(s, PrimOp.lit(0)),
                Vars.write(addL, PrimOp.readMatFrame(Vars.read(1), add)),
                Vars.write(subL, PrimOp.readMatFrame(Vars.read(1), sub)),
                Vars.write(ltL, PrimOp.readMatFrame(Vars.read(1), lt)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(Vars.read(ltL),
                                            PrimOp.lit(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    Vars.read(addL),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    Vars.read(subL),
                                                    Vars.read(i), PrimOp.lit(1)))
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
                                array(PrimOp.lit(PrimOp.ADD), PrimOp.lit(PrimOp.SUB), PrimOp.lit(PrimOp.LT)),
                                trampoFd
                        )
                )
        ));
        return trampo;
    }

    // Same as using the parent's mat frame (4x slow down). Not quite good.
    private static Closure createPassingPrimOpsInFreshMatFrame() {
        Closure trampo = Closure.empty("loop-pass-primops-trampo");
        Closure loop = Closure.empty("loop-pass-primops");

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
                Vars.write(s, PrimOp.lit(0)),
                Vars.write(arr, Vars.read(1)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(PrimOp.readMatFrame(Vars.read(arr), lt),
                                            PrimOp.lit(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), add),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), sub),
                                                    Vars.read(i), PrimOp.lit(1)))
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
                                array(PrimOp.lit(PrimOp.ADD), PrimOp.lit(PrimOp.SUB), PrimOp.lit(PrimOp.LT)),
                                trampoFd
                        )
                )
        ));
        return trampo;
    }

    // Primops function passed in an mat frame.
    // This incurs some (~4x) slow downs, but might be still acceptable. Note sure why
    // this can't be optimized...
    private static Closure createPassingPrimOpsInParentsMatFrame() {
        Closure trampo = Closure.empty("loop-pass-primops-trampo");
        Closure loop = Closure.empty("loop-pass-primops");

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
                Vars.write(s, PrimOp.lit(0)),
                Vars.write(arr, Vars.read(1)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(PrimOp.readMatFrame(Vars.read(arr), lt),
                                            PrimOp.lit(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), add),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    PrimOp.readMatFrame(Vars.read(arr), sub),
                                                    Vars.read(i), PrimOp.lit(1)))
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
                Vars.write(add, PrimOp.lit(PrimOp.ADD)),
                Vars.write(sub, PrimOp.lit(PrimOp.SUB)),
                Vars.write(lt, PrimOp.lit(PrimOp.LT)),
                ApplyNode.known(loop,
                        Vars.read(0),
                        PrimOp.matCurrentFrame())
        ), trampoFd));
        return trampo;
    }

    // Primops function passed in an array arg. Also works - Graal's loop-based optimizations
    // are truly great.
    private static Closure createPassingPrimOpsInArray() {
        Closure trampo = Closure.empty("loop-pass-primops-trampo");
        Closure loop = Closure.empty("loop-pass-primops");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        FrameSlot arr = fd.addFrameSlot("arr");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.lit(0)),
                Vars.write(arr, Vars.read(1)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(PrimOp.readArray(Vars.read(arr), 2),
                                            PrimOp.lit(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    PrimOp.readArray(Vars.read(arr), 0),
                                                    Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    PrimOp.readArray(Vars.read(arr), 1),
                                                    Vars.read(i), PrimOp.lit(1)))
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
                        PrimOp.lit(array(PrimOp.ADD, PrimOp.SUB, PrimOp.LT)))));
        return trampo;
    }

    // Primops function passed in args. Works well.
    private static Closure createPassingPrimOps() {
        Closure trampo = Closure.empty("loop-pass-primops-trampo");
        Closure loop = Closure.empty("loop-pass-primops");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");
        FrameSlot add = fd.addFrameSlot("add");
        FrameSlot sub = fd.addFrameSlot("sub");
        FrameSlot lt = fd.addFrameSlot("lt");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.lit(0)),
                Vars.write(add, Vars.read(1)),
                Vars.write(sub, Vars.read(2)),
                Vars.write(lt, Vars.read(3)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    ApplyNode.unknown(Vars.read(lt), PrimOp.lit(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, ApplyNode.unknown(
                                                    Vars.read(add), Vars.read(s), Vars.read(i))),
                                            Vars.write(i, ApplyNode.unknown(
                                                    Vars.read(sub), Vars.read(i), PrimOp.lit(1)))
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
                        PrimOp.lit(PrimOp.ADD),
                        PrimOp.lit(PrimOp.SUB),
                        PrimOp.lit(PrimOp.LT))));
        return trampo;
    }

    // With boxed long, repeatingNode and primop and var read/write nodes.
    private static Closure createFaster() {
        Closure loop = Closure.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");

        class LoopBody extends Node implements RepeatingNode {
            @Child Expr check = PrimOp.lt(PrimOp.lit(0L), Vars.read(i));
            Expr addSI = Vars.write(s, PrimOp.add(Vars.read(s), Vars.read(i)));
            Expr subI1 = Vars.write(i, PrimOp.sub(Vars.read(i), PrimOp.lit(1)));
            @Child Expr body = Begin.create(addSI, subI1);

            @Override
            public boolean executeRepeating(VirtualFrame frame) {
                try {
                    if (check.executeBoolean(frame)) {
                        body.executeGeneric(frame);
                        return true;
                    } else {
                        return false;
                    }
                } catch (UnexpectedResultException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Expr simpleReadS = new Expr() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                try {
                    return frame.getObject(s);
                } catch (FrameSlotTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(0)),
                Vars.write(s, PrimOp.lit(0L)),
                new Expr() {
                    @Child private LoopNode loopBody = Yasir.rt().createLoopNode(new LoopBody());
                    @Child private Expr result = Vars.read(s);
                    @Override
                    public Object executeGeneric(VirtualFrame frame) {
                        // frame.setObject(i, frame.getArguments()[0]);
                        // frame.setObject(s, 0L);
                        loopBody.executeLoop(frame);

                        // try {
                        //     return frame.getObject(s);
                        // } catch (FrameSlotTypeException e) {
                        //     throw new RuntimeException(e);
                        // }
                        return result.executeGeneric(frame);
                    }
        }), fd));

        return loop;
    }

    // With boxed long and repeating node.
    private static Closure createFastestBoxed() {
        Closure loop = Closure.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");

        class LoopBody extends Node implements RepeatingNode {
            @Override
            public boolean executeRepeating(VirtualFrame frame) {
                try {
                    if ((Long) frame.getValue(i) > 0) {
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
                try {
                    return frame.getObject(s);
                } catch (FrameSlotTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        }, fd));

        return loop;
    }

    // With unboxed long and repeating node.
    // Not sure why this is slower than the boxed version...
    private static Closure createFastest() {
        Closure loop = Closure.empty("loop-fast");

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
    private static Closure createStandard() {
        Closure trampo = Closure.empty("loop-fast-trampo");

        // [0]: closure ptr, [1]: i, [2]: s
        Closure loop = Closure.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");

        loop.setTarget(Yasir.createCallTarget(Begin.create(
                Vars.write(i, Vars.read(1)),
                Vars.write(s, PrimOp.lit(0)),
                new Expr() {
                    @Child
                    LoopNode loopBody = Yasir.rt().createLoopNode(
                            new FastRepNode(
                                    PrimOp.lt(PrimOp.lit(0), Vars.read(i)),
                                    Begin.create(
                                            Vars.write(s, PrimOp.add(Vars.read(s), Vars.read(i))),
                                            Vars.write(i, PrimOp.sub(Vars.read(i), PrimOp.lit(1)))
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
                ApplyNode.unknownWithPayload(PrimOp.lit(loop), Vars.read(0))));
        return trampo;
    }

    static class FastRepNode extends Node implements RepeatingNode {
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
