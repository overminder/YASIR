import Begin.BeginCont
import com.oracle.truffle.api.{RootCallTarget, Truffle}
import com.oracle.truffle.api.frame._
import com.oracle.truffle.api.nodes.{Node, RootNode}

abstract class Expr(frameDescr: FrameDescriptor = null) extends RootNode(classOf[YasirLanguage], null, frameDescr) {
  abstract def evaluate(frame: Frame, cont: Cont): CekState
  sealed override def execute(frame: VirtualFrame): AnyRef = {
    val cont = frame.getArguments()(1).asInstanceOf[Cont]
    throw TrampolineException(evaluate(frame, cont))
  }
}

sealed case class ConstInt(v: Int) extends Expr {
  def evaluate(frame: Frame, cont: Cont): CekState = {
    cont.plugReduce(Int.box(v), frame)
  }
}

sealed case class ConstAnyRef(o: AnyRef) extends Expr {
  def evaluate(frame: Frame, cont: Cont): CekState = {
    cont.plugReduce(o, frame)
  }
}

object Add {
  def apply(lhs: Expr, rhs: Expr): Expr = {
    Apply(ConstAnyRef(PrimAdd), Array(lhs, rhs))
  }
}

object Sub {
  def apply(lhs: Expr, rhs: Expr): Expr = {
    Apply(ConstAnyRef(PrimSub), Array(lhs, rhs))
  }
}

object LessThan {
  def apply(lhs: Expr, rhs: Expr): Expr = {
    Apply(ConstAnyRef(PrimLessThan), Array(lhs, rhs))
  }
}

sealed case class MkLambda(name: String, argNames: Array[FrameSlot], body: Expr, frameDescr: FrameDescriptor) extends Expr {
  override def evaluate(frame: Frame, cont: Cont): CekState = {
    cont.plugReduce(Lambda(this, frame.materialize()), frame)
  }

  def enterWithArgs(capturedFrame: MaterializedFrame, args: Array[AnyRef], cont: Cont): CekState = {
    val realArgs = capturedFrame +: args
    val frame = Truffle.getRuntime().createVirtualFrame(realArgs, frameDescr)
    for ((argName, i) <- argNames.zipWithIndex) {
      frame.setObject(argName, frame.getArguments()(i + 1))
    }
    CekState(body, frame, cont)
  }
}

sealed case class Apply(func: Expr, args: Array[Expr]) extends Expr {
  override def evaluate(frame: Frame, cont: Cont): CekState = {
    CekState(func, frame, Apply.ApplyCont(this, None, Array(), cont))
  }
}

object Apply {
  sealed case class ApplyCont(ap: Apply,
                              funcValue: Option[Callable],
                              argValues: Array[AnyRef],
                              cont: Cont) extends Cont {
    override def plugReduce(value: AnyRef, frame: Frame): CekState = {
      val (nextFuncValue, nextArgValues) = funcValue match {
        case None => (value.asInstanceOf[Callable], argValues)
        case Some(x) => (x, argValues :+ value)
      }
      if (nextArgValues.length >= ap.args.length) {
        // Saturated.
        if (cont.isInstanceOf[ReturnCont]) {
          // TCO.
          nextFuncValue.call(nextArgValues, frame, cont)
        } else {
          // Need to save the current frame.
          nextFuncValue.call(nextArgValues, frame, ReturnCont(frame.materialize(), cont))
        }
      } else {
        CekState(ap.args(nextArgValues.length), frame, ApplyCont(ap, Some(nextFuncValue), nextArgValues, cont))
      }
    }
  }

  sealed case class ReturnCont(frame: Frame, cont: Cont) extends Cont {
    override def plugReduce(value: AnyRef, unusedFrame: Frame): CekState = {
      ReturnValue(value, frame, cont)
    }
  }
}

object ReturnValue {
  def apply(value: AnyRef, frame: Frame, cont: Cont): CekState = {
    CekState(Label, frame, LabelCont(value, frame, cont))
  }

  case object Label extends Expr {
    override def evaluate(frame: Frame, cont0: Cont): CekState = {
      val cont = cont0.asInstanceOf[LabelCont]
      cont.cont.plugReduce(cont.value, cont.frame)
    }
  }

  case class LabelCont(value: AnyRef, frame: Frame, cont: Cont) extends Cont {
    override def plugReduce(value: AnyRef, frame: Frame): CekState = {
      throw new Exception("Shouldn't be reachable")
    }
  }
}

sealed case class Begin(exprs: Array[Expr]) extends Expr {
  override def evaluate(frame: Frame, cont: Cont): CekState = {
    if (exprs.length == 0) {
      cont.plugReduce(Symbol("#void"), frame)
    } else {
      CekState(exprs(0), frame, Begin.BeginCont(this, 1, cont))
    }
  }
}

object Begin {
  sealed case class BeginCont(b: Begin, nextIx: Int, cont: Cont) extends Cont {
    override def plugReduce(value: AnyRef, frame: Frame): CekState = {
      if (nextIx >= b.exprs.length) {
        cont.plugReduce(value, frame)
      } else {
        CekState(b.exprs(nextIx), frame, BeginCont(b, nextIx + 1, cont))
      }
    }
  }
}

object Frames {
  def atDepth(frame: Frame, depth: Int): Frame = {
    var here = frame
    var mutDepth = depth
    while (mutDepth > 0) {
      here = here.getArguments()(0).asInstanceOf[Frame]
      mutDepth -= 1
    }
    here
  }
}

sealed case class WriteVar(expr: Expr, slot: FrameSlot, depth: Int = 0) extends Expr {
  override def evaluate(frame: Frame, cont: Cont): CekState = {
    CekState(expr, frame, WriteVar.WriteVarCont(this, cont))
  }
}

object WriteVar {
  sealed case class WriteVarCont(wv: WriteVar, cont: Cont) extends Cont {
    def plugReduce(value: AnyRef, frame: Frame): CekState = {
      Frames.atDepth(frame, wv.depth).setObject(wv.slot, value)
      cont.plugReduce(Symbol("#void"), frame)
    }
  }
}

// TODO: Specialize this.
sealed case class ReadVar(slot: FrameSlot, depth: Int = 0) extends Expr {
  override def evaluate(frame: Frame, cont: Cont): CekState = {
    cont.plugReduce(Frames.atDepth(frame, depth).getObject(slot), frame)
  }
}

sealed case class If(c: Expr, t: Expr, f: Expr) extends Expr {
  override def evaluate(frame: Frame, cont: Cont): CekState = {
    CekState(c, frame, If.IfCont(this, cont))
  }
}

object If {
  sealed case class IfCont(i: If, cont: Cont) extends Cont {
    def plugReduce(value: AnyRef, frame: Frame): CekState = {
      if (value eq java.lang.Boolean.FALSE.asInstanceOf[AnyRef]) {
        CekState(i.f, frame, cont)
      } else {
        CekState(i.t, frame, cont)
      }
    }
  }
}