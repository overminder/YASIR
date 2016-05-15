import com.oracle.truffle.api.frame.FrameDescriptor

object Example {
  def makeFibo(n: Int): Expr = {
    val mainFd = Rt.createEnvDescr()
    val fiboFd = Rt.createEnvDescr()
    val fiboSlot = mainFd.addFrameSlot("fiboSlot")
    val nSlot = fiboFd.addFrameSlot("n")

    val fiboBody = MkLambda("fibo", Array(nSlot),
      If(
        LessThan(ReadVar(nSlot), ConstInt(2)),
        ReadVar(nSlot),
        Add(
          Apply(ReadVar(fiboSlot, 1), Array(Sub(ReadVar(nSlot), ConstInt(1)))),
          Apply(ReadVar(fiboSlot, 1), Array(Sub(ReadVar(nSlot), ConstInt(2)))))),
      fiboFd)

    val main = MkLambda("main", Array(),
      Begin(
        Array(
          WriteVar(fiboBody, fiboSlot),
          Apply(ReadVar(fiboSlot), Array(ConstInt(n))))),
      mainFd)

    Apply(main, Array())
  }
}
