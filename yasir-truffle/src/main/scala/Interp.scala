import Halt.HaltException
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
  sealed case class Run(expr: Expr) extends RootNode(classOf[YasirLanguage], null, null) {
    override def execute(frame: VirtualFrame): AnyRef = {
      runInternal(expr)
    }
  }

  def run(expr: Expr): AnyRef = {
    Truffle.getRuntime().createCallTarget(Run(expr)).call()
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


sealed case class CekState(expr: Expr, env: Frame, cont: Cont)

case object Halt extends Cont {
  def plugReduce(value: AnyRef, frame: Frame): CekState = {
    throw new HaltException(value)
  }

  case class HaltException(value: AnyRef) extends RuntimeException
}
