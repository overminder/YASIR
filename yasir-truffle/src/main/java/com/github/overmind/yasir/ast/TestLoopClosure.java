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

// Loops and tail calls.
public final class TestLoopClosure {
    public static Closure create() {
        return createFastest();
    }

    public static long call(long n) {
        return (Long) Yasir.createCallTarget(ApplyNode.known(createFast(), PrimOp.lit(n))).call();
    }

    // With boxed long.
    private static Closure createFastestBoxed() {
        Closure loop = Closure.empty("loop-fast");

        FrameDescriptor fd = new FrameDescriptor();
        FrameSlot i = fd.addFrameSlot("i");
        FrameSlot s = fd.addFrameSlot("s");

        class LoopBody extends Node implements RepeatingNode {
            @Override
            public boolean executeRepeating(VirtualFrame frame) {
                try {
                    frame.setObject(s, (Long) frame.getObject(i) + (Long) frame.getObject(s));
                    if ((Long) frame.getValue(i) > 0) {
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

    // With unboxed long.
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

    // Done in the standard Truffle way.
    private static Closure createFast() {
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
