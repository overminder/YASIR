scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "com.oracle.truffle" % "truffle-api" % "0.13",
  "com.oracle.truffle" % "truffle-dsl-processor" % "0.13",
  "junit" % "junit" % "4.12" % "test"
)

managedSourceDirectories in Compile += baseDirectory.value / "target" / "scala-2.10" / "classes"

// So that the annotation processors can run.
compileOrder := CompileOrder.JavaThenScala

import java.util.Properties

val localProperties = settingKey[Properties]("The application properties")

localProperties := {
  val prop = new Properties()
  IO.load(prop, new File("local.properties"))
  prop
}

javaOptions += localProperties.value.getProperty("bootcpOpt")

javaOptions ++= Seq(
  // These used to be '-G:+${NAME}' (and require -XX:+UnlockDiagnosticVMOptions)
  // rather than '-Dgraal.${NAME}=true'...
  "-Dgraal.TraceTruffleCompilationDetails=true",
  // "-Dgraal.TraceTruffleInlining=true",
  // "-Dgraal.TraceTruffleTransferToInterpreter=true",
  "-Dgraal.TraceTrufflePerformanceWarnings=true",
  "-Dgraal.Dump=",
  "-Dgraal.TruffleBackgroundCompilation=false"
)

// To apply javaOptions
fork in run := true

mainClass in (Compile, run) := Some("com.github.overmind.yasir.Main")
