package com.github.overmind.yasir.ast;

import com.github.overmind.yasir.value.Closure;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

public abstract class DispatchClosureNode extends Node {
    protected static final int INLINE_CACHE_SIZE = 2;

    public abstract Object executeDispatch(VirtualFrame frame,
                                           Closure function,
                                           Object[] arguments);

    @Specialization(limit = "INLINE_CACHE_SIZE", guards = "function.target() == cachedFunction.target()", assumptions = "cachedFunction.targetNotChanged()")
    protected static Object doDirect(VirtualFrame frame, Closure function, Object[] arguments, //
                                     @Cached("function") Closure cachedFunction, //
                                     @Cached("create(cachedFunction.target())") DirectCallNode callNode) {
    /* Inline cache hit, we are safe to execute the cached call target. */
        return callNode.call(frame, arguments);
    }

    /**
     * FullyTruffled-path code for a call, used when the polymorphic inline cache exceeded its maximum size
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
