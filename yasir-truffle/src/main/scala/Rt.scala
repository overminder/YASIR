import scala.collection.mutable

/**
  * Created by overmind on 5/15/16.
  */

object TruffleRt {
  import com.oracle.truffle.api.frame._

  type Env = Frame
  type Slot = FrameSlot
  type Descr = FrameDescriptor

  def atDepth(frame: Env, depth: Int): Env = {
    var here = frame
    var mutDepth = depth
    while (mutDepth > 0) {
      here = here.getArguments()(0).asInstanceOf[Env]
      mutDepth -= 1
    }
    here
  }
}

object SimpleRt {
  type Slot = Int
  sealed class Descr() {
    var nextIx = 0
    val nameMap = mutable.HashMap.empty[String, Slot]
    def addFrameSlot(name: String): Slot = {
      nameMap.getOrElseUpdate(name, {
        val res = nextIx
        nextIx += 1
        res
      })
    }
  }

  sealed case class Env(slots: Array[AnyRef], prev: Option[Env]) {
    def getObject(slot: Slot): AnyRef = {
      slots(slot)
    }
  }
}

val Rt = SimpleRt
