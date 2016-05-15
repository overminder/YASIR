import com.oracle.truffle.api.{RootCallTarget, Truffle}
import com.oracle.truffle.api.frame._
import com.oracle.truffle.api.nodes.{Node, RepeatingNode, RootNode}

abstract class Expr extends RootNode(classOf[YasirLanguage], null, null) {
  def evaluate(env: Rt.Env, cont: Cont): CekState

  override def execute(frame: VirtualFrame): AnyRef = {
    val env = frame.getArguments()(0).asInstanceOf[Rt.Env]
    val cont = frame.getArguments()(1).asInstanceOf[Cont]
    throw TrampolineException(evaluate(env, cont))
  }
}


sealed case class ConstInt(v: Int) extends Expr {
  def evaluate(env: Rt.Env, cont: Cont): CekState = {
    cont.plugReduce(Int.box(v), env)
  }
}

sealed case class ConstAnyRef(o: AnyRef) extends Expr {
  def evaluate(env: Rt.Env, cont: Cont): CekState = {
    cont.plugReduce(o, env)
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

sealed case class MkLambda(name: String, argNames: Array[Rt.Slot], body: Expr, envDescr: Rt.EnvDescr) extends Expr {
  val target = Rt.createCallTarget(body)
  override def evaluate(env: Rt.Env, cont: Cont): CekState = {
    cont.plugReduce(Lambda(this, env.materialize()), env)
  }

  def enterWithArgs(capturedEnv: Rt.Env, args: Array[AnyRef], cont: Cont): CekState = {
    val env = Rt.createEnv(args, capturedEnv, envDescr)
    for ((argName, i) <- argNames.zipWithIndex) {
      env.setObject(argName, env.getArguments()(i + 1))
    }
    CekState(target, env, cont)
  }
}

sealed case class Apply(func: Expr, args: Array[Expr], tail: Boolean = false) extends Expr {
  val funcTarget = Rt.createCallTarget(func)
  val argTargets = args.map(Rt.createCallTarget)

  override def evaluate(frame: Rt.Env, cont: Cont): CekState = {
    CekState(funcTarget, frame, Apply.ApplyCont(this, None, Array(), cont))
  }
}

object Apply {
  sealed case class ApplyCont(ap: Apply,
                              funcValue: Option[Callable],
                              argValues: Array[AnyRef],
                              cont: Cont) extends Cont {
    override def plugReduce(value: AnyRef, env: Rt.Env): CekState = {
      val (nextFuncValue, nextArgValues) = funcValue match {
        case None => (value.asInstanceOf[Callable], argValues)
        case Some(x) => (x, argValues :+ value)
      }
      if (nextArgValues.length >= ap.args.length) {
        // Saturated.
        if (ap.tail) {
          // TCO.
          nextFuncValue.call(nextArgValues, env, cont)
        } else {
          // Need to save the current frame.
          nextFuncValue.call(nextArgValues, env, ReturnCont(env.materialize(), cont))
        }
      } else {
        CekState(
          ap.argTargets(nextArgValues.length),
          env,
          ApplyCont(ap, Some(nextFuncValue), nextArgValues, cont))
      }
    }
  }

  sealed case class ReturnCont(env: Rt.Env, cont: Cont) extends Cont {
    override def plugReduce(value: AnyRef, unusedEnv: Rt.Env): CekState = {
      ReturnValue(value, env, cont)
    }
  }
}

object ReturnValue {
  def apply(value: AnyRef, frame: Rt.Env, cont: Cont): CekState = {
    CekState(Label.target, frame, LabelCont(value, frame, cont))
  }

  private case object Label extends Expr {
    val target = Rt.createCallTarget(this)
    override def evaluate(frame: Rt.Env, cont0: Cont): CekState = {
      val cont = cont0.asInstanceOf[LabelCont]
      cont.cont.plugReduce(cont.value, cont.frame)
    }
  }

  private case class LabelCont(value: AnyRef, frame: Rt.Env, cont: Cont) extends Cont {
    override def plugReduce(value: AnyRef, frame: Rt.Env): CekState = {
      throw new Exception("Shouldn't be reachable")
    }
  }
}

sealed case class Begin(exprs: Array[Expr]) extends Expr {
  val targets = exprs.map(Rt.createCallTarget)
  override def evaluate(frame: Rt.Env, cont: Cont): CekState = {
    if (exprs.length == 0) {
      cont.plugReduce(Symbol("#void"), frame)
    } else {
      CekState(targets(0), frame, Begin.BeginCont(this, 1, cont))
    }
  }
}

object Begin {
  sealed case class BeginCont(b: Begin, nextIx: Int, cont: Cont) extends Cont {
    override def plugReduce(value: AnyRef, frame: Rt.Env): CekState = {
      if (nextIx >= b.exprs.length) {
        cont.plugReduce(value, frame)
      } else {
        CekState(b.targets(nextIx), frame, BeginCont(b, nextIx + 1, cont))
      }
    }
  }
}

sealed case class WriteVar(expr: Expr, slot: Rt.Slot, depth: Int = 0) extends Expr {
  val target = Rt.createCallTarget(expr)
  override def evaluate(frame: Rt.Env, cont: Cont): CekState = {
    CekState(target, frame, WriteVar.WriteVarCont(this, cont))
  }
}

// TODO: Specialize this.
object WriteVar {
  sealed case class WriteVarCont(wv: WriteVar, cont: Cont) extends Cont {
    def plugReduce(value: AnyRef, frame: Rt.Env): CekState = {
      Rt.atDepth(frame, wv.depth).setObject(wv.slot, value)
      cont.plugReduce(Symbol("#void"), frame)
    }
  }
}

// TODO: And this
sealed case class ReadVar(slot: Rt.Slot, depth: Int = 0) extends Expr {
  override def evaluate(frame: Rt.Env, cont: Cont): CekState = {
    cont.plugReduce(Rt.atDepth(frame, depth).getObject(slot), frame)
  }
}

sealed case class If(c: Expr, t: Expr, f: Expr) extends Expr {
  val cTarget = Rt.createCallTarget(c)
  val tTarget = Rt.createCallTarget(t)
  val fTarget = Rt.createCallTarget(f)
  override def evaluate(frame: Rt.Env, cont: Cont): CekState = {
    CekState(cTarget, frame, If.IfCont(this, cont))
  }
}

object If {
  sealed case class IfCont(i: If, cont: Cont) extends Cont {
    def plugReduce(value: AnyRef, frame: Rt.Env): CekState = {
      if (value eq java.lang.Boolean.FALSE.asInstanceOf[AnyRef]) {
        CekState(i.fTarget, frame, cont)
      } else {
        CekState(i.tTarget, frame, cont)
      }
    }
  }
}