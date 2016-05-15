
object Main {
  def main(args: Array[String]): Unit = {
    val n = 20
    val fiboN = Example.makeFibo(n)
    for (i <- 1 to 5) {
      bench(s"fibo($n)", () => {
        val res = Interp.run(fiboN)
        println(s"res = $res")
      })
    }
  }

  // Explicit thunk since explicit is better than implicit.
  def bench(name: String, thunk: () => Unit): Unit = {
    val t0 = System.nanoTime()
    thunk()
    val t1 = System.nanoTime()
    println(s"${name} took ${(t1 - t0) / 1000000.0} millis")
  }
}


