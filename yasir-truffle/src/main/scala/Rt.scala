import com.oracle.truffle.api.{CallTarget, Truffle}
import com.oracle.truffle.api.nodes.RootNode

import scala.collection.mutable

/**
  * Created by overmind on 5/15/16.
  */

object Rt {
  import com.oracle.truffle.api.frame._

  type Env = Frame
  type Slot = FrameSlot
  type EnvDescr = FrameDescriptor

  def atDepth(frame: Env, depth: Int): Env = {
    var here = frame
    var mutDepth = depth
    while (mutDepth > 0) {
      here = here.getArguments()(0).asInstanceOf[Env]
      mutDepth -= 1
    }
    here
  }

  val emptyEnv: Env = Truffle.getRuntime().createVirtualFrame(Array(), createEnvDescr())

  def createEnv(args: Array[AnyRef], prev: Env, descr: EnvDescr): Env = {
    Truffle.getRuntime().createVirtualFrame(prev +: args, descr)
  }

  def createEnvDescr(): EnvDescr = new FrameDescriptor()

  def createCallTarget(node: RootNode): CallTarget = Truffle.getRuntime().createCallTarget(node)
}

object SimpleRt {
  type Slot = Int
  sealed case class EnvDescr() {
    private var nextIx = 0
    private val nameMap = mutable.HashMap.empty[String, Slot]

    def length = nextIx
    def addFrameSlot(name: String): Slot = {
      nameMap.getOrElseUpdate(name, {
        val res = nextIx
        nextIx += 1
        res
      })
    }
  }

  def atDepth(frame: Env, depth: Int): Env = {
    var here = frame
    var mutDepth = depth
    while (mutDepth > 0) {
      here = here.getArguments()(0).asInstanceOf[Env]
      mutDepth -= 1
    }
    here
  }

  trait Env {
    def getObject(slot: Slot): AnyRef
    def setObject(slot: Slot, o: AnyRef)
    def getArguments(): Array[AnyRef]
    def materialize(): Env = this
  }

  case object EmptyEnv extends Env {
    def getObject(slot: Slot): AnyRef = ???
    def setObject(slot: Slot, o: AnyRef) = ???
    def getArguments(): Array[AnyRef] = ???
  }

  sealed case class ConcreteEnv(args: Array[AnyRef], locals: Array[AnyRef]) extends Env {
    def getObject(slot: Slot): AnyRef = locals(slot)
    def setObject(slot: Slot, o: AnyRef) = locals(slot) = o
    def getArguments(): Array[AnyRef] = args
  }

  def emptyEnv: Env = EmptyEnv

  def createEnv(args: Array[AnyRef], prev: Env, descr: EnvDescr): Env = {
    ConcreteEnv(prev +: args, Array.fill[AnyRef](descr.length)(null))
  }

  def createEnvDescr(): EnvDescr = EnvDescr()

  def createCallTarget(node: RootNode): CallTarget = Truffle.getRuntime().createCallTarget(node)
}

