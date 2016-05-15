import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.dsl.TypeSystem
import com.oracle.truffle.api.frame.{Frame, MaterializedFrame}

import scala.collection.mutable

sealed trait Symbol {
  def name: String
}

case object Symbol {
  private sealed case class InternedSymbol(name: String) extends Symbol {
  }
  private val internedSymbols = mutable.HashMap.empty[String, Symbol]
  def apply(name: String): Symbol = {
    internedSymbols.getOrElseUpdate(name, InternedSymbol(name))
  }
}

trait Callable {
  def call(args: Array[AnyRef], env: Rt.Env, cont: Cont): CekState
}

case object PrimAdd extends Callable {
  override def call(args: Array[AnyRef], env: Rt.Env, cont: Cont): CekState = {
    val sum = args.map(_.asInstanceOf[Int]).sum
    cont.plugReduce(Int.box(sum), env)
  }
}

case object PrimSub extends Callable {
  override def call(args: Array[AnyRef], env: Rt.Env, cont: Cont): CekState = {
    args.map(_.asInstanceOf[Int]) match {
      case Array(lhs, rhs) => cont.plugReduce(Int.box(lhs - rhs), env)
    }
  }
}

case object PrimLessThan extends Callable {
  override def call(args: Array[AnyRef], env: Rt.Env, cont: Cont): CekState = {
    args.map(_.asInstanceOf[Int]) match {
      case Array(lhs, rhs) => cont.plugReduce(Boolean.box(lhs < rhs), env)
    }
  }
}

sealed case class Lambda(info: MkLambda, capturedEnv: Rt.Env) extends Callable {
  override def call(args: Array[AnyRef], env: Rt.Env, cont: Cont): CekState = {
    info.enterWithArgs(capturedEnv, args, cont)
  }
}

@TypeSystem(value = Array(classOf[Long], classOf[Boolean], classOf[Symbol], classOf[Callable]))
abstract class YasirTypes {
}
