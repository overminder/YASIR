import com.oracle.truffle.api.TruffleLanguage.Env
import com.oracle.truffle.api.frame.{Frame, FrameDescriptor, MaterializedFrame, VirtualFrame}
import com.oracle.truffle.api.nodes.{Node, RepeatingNode, RootNode}
import com.oracle.truffle.api.source.Source
import com.oracle.truffle.api.{CallTarget, Truffle, TruffleLanguage}

case class YasirContext() {

}

case class YasirLanguage() extends TruffleLanguage[YasirContext] {
  override def findExportedSymbol(context: YasirContext, globalName: String, onlyExplicit: Boolean): AnyRef = ???

  override def getLanguageGlobal(context: YasirContext): AnyRef = ???

  override def isObjectOfLanguage(`object`: scala.Any): Boolean = ???

  override def parse(code: Source, context: Node, argumentNames: String*): CallTarget = ???

  override def createContext(env: Env): Nothing = ???

  override def evalInContext(source: Source, node: Node, mFrame: MaterializedFrame): AnyRef = ???
}

sealed case class CekState(expr: CallTarget, frame: Rt.Env, cont: Cont) {
  def toTuple: (CallTarget, Rt.Env, Cont) = (expr, frame, cont)
}

case object Halt extends Cont {
  def plugReduce(value: AnyRef, env: Rt.Env): CekState = {
    throw new HaltException(value)
  }
}

object Interp {
  def run(expr: Expr): AnyRef = {
    var target: CallTarget = Truffle.getRuntime().createCallTarget(expr)
    var frame = Rt.emptyEnv
    var cont: Cont = Halt
    while (true) {
      try {
        target.call(frame, cont)
      } catch {
        case HaltException(value) => return value
        case TrampolineException(state) => {
          val (target1, frame1, cont1) = state.toTuple
          target = target1
          frame = frame1
          cont = cont1
        }
      }
    }

    throw new Exception("Shouldn't be reachable")
  }
}

case class HaltException(value: AnyRef) extends Exception
case class TrampolineException(newState: CekState) extends Exception
