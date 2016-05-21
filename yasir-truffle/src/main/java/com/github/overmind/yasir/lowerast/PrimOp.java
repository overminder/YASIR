package com.github.overmind.yasir.lowerast;

import com.github.overmind.yasir.ast.Expr;
import com.github.overmind.yasir.ast.PrimOpsFactory;
import com.github.overmind.yasir.value.Box;
import com.github.overmind.yasir.value.Closure;
import com.github.overmind.yasir.value.Nil;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;

public final class PrimOp {
    public static Expr add(Expr lhs, Expr rhs) {
        return PrimOpFactory.AddNodeGen.create(lhs, rhs);
    }

    public static Expr lit(long v) {
        return PrimOpFactory.LongLitNodeGen.create(v);
    }

    public static Expr lit(Closure c) {
        return PrimOpFactory.ClosureLitNodeGen.create(c);
    }

    public static Expr sub(Expr lhs, Expr rhs) {
        return PrimOpFactory.SubNodeGen.create(lhs, rhs);
    }

    public static Expr lt(Expr lhs, Expr rhs) {
        return PrimOpFactory.LtNodeGen.create(lhs, rhs);
    }

    public static Expr box(Expr v) {
        return PrimOpFactory.MkBoxNodeGen.create(v);
    }

    public static Expr readBox(Expr v) {
        return PrimOpFactory.ReadBoxNodeGen.create(v);
    }

    public static Expr writeBox(Expr lhs, Expr rhs) {
        return PrimOpFactory.WriteBoxNodeGen.create(lhs, rhs);
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
}
