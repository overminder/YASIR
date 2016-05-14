import com.oracle.truffle.api.frame.Frame

trait Cont {
  def plugReduce(value: Object, frame: Frame): CekState
}
