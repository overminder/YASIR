import com.oracle.truffle.api.TruffleLanguage.Env
import com.oracle.truffle.api.frame.{Frame, FrameDescriptor, MaterializedFrame, VirtualFrame}
import com.oracle.truffle.api.nodes.{Node, RootNode}
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

object Interp {
  def run(expr: Expr): AnyRef = {
    var target: CallTarget = Truffle.getRuntime().createCallTarget(expr)
    var frame: Frame = null
    var cont: Cont = Halt
    while (true) {
      try {
        target.call(frame, cont)
      } catch {
        case HaltException(value) => return value
        case TrampolineException(state) => state match {
          case CekState.ReuseFrame(target1, cont1) => {
            target = target1
            cont = cont1
          }
          case CekState.ChangeFrame(target1, frame1, cont1) => {
            target = target1
            frame = frame1
            cont = cont1
          }
        }
      }
    }
  }

  private def runInternal(expr0: Expr): AnyRef = {
    var expr = expr0
    var env: Frame = Truffle.getRuntime().createVirtualFrame(Array(), new FrameDescriptor())
    var cont: Cont = Halt
    while (true) {
      try {
        expr.evaluate(env, cont) match {
          case CekState(expr1, env1, cont1) => {
            expr = expr1
            env = env1
            cont = cont1
          }
        }
      } catch {
        case HaltException(value) => return value
      }
    }
    throw new Exception("Shouldn't be reachable")
  }
}


sealed trait CekState
object CekState {
  sealed case class ReuseFrame(expr: CallTarget, cont: Cont) extends CekState
  sealed case class ChangeFrame(expr: CallTarget, frame: Frame, cont: Cont) extends CekState
}

case object Halt extends Cont {
  def plugReduce(value: AnyRef, frame: Frame): CekState = {
    throw new HaltException(value)
  }
}

case class HaltException(value: AnyRef) extends Exception
case class TrampolineException(newState: CekState) extends Exception
