libraryDependencies ++= Seq(
  "com.oracle.truffle" % "truffle-api" % "0.13",
  "com.oracle.truffle" % "truffle-dsl-processor" % "0.13",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

javaOptions ++= Seq(
  "-XX:+UnlockDiagnosticVMOptions",
  "-G:+TraceTruffleCompilation",
  "-G:+TraceTruffleInlining",
  "-G:+TraceTruffleTransferToInterpreter"
)

// To apply javaOptions
fork := true
