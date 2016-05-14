import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import org.scalatest.{FlatSpec, Matchers}

class ExprSpec extends FlatSpec with Matchers {
  "ConstInt" should "evaluate to a constant int" in {
    val expr = ConstInt(42)
    Interp.run(expr) shouldBe 42
  }

  "Add" should "add its arguments" in {
    val expr = Add(ConstInt(40), ConstInt(2))
    Interp.run(expr) shouldBe 42
  }

  "Simple lambda" should "work" in {
    val frameDescr = new FrameDescriptor()
    val x = frameDescr.addFrameSlot("x")
    val id = MkLambda("id", Array(x), ReadVar(x), frameDescr)
    val expr = Apply(id, Array(ConstInt(42)))
    Interp.run(expr) shouldBe 42
  }

  "Lexical scope" should "work" in {
    val outer = new FrameDescriptor()
    val inner = new FrameDescriptor()
    val x = outer.addFrameSlot("x")
    val y = inner.addFrameSlot("y")
    val k = MkLambda("const", Array(x), MkLambda("const-inner", Array(y), ReadVar(x, 1), inner), outer)
    val expr = Apply(Apply(k, Array(ConstInt(42))), Array(ConstInt(0)))
    Interp.run(expr) shouldBe 42
  }

  "WriteVar" should "work" in {
    val fd = new FrameDescriptor()
    val x = fd.addFrameSlot("x")
    val fn = MkLambda("set-42", Array(x), Begin(Array(WriteVar(ConstInt(42), x), ReadVar(x))), fd)
    val expr = Apply(fn, Array(ConstInt(0)))
    Interp.run(expr) shouldBe 42
  }

  "Fibo" should "work" in {
    Interp.run(Example.makeFibo(10)) shouldBe 55
  }
}