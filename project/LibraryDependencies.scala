import sbt._

object LibraryDependencies {
  val catsFree = "org.typelevel" %% "cats-free" % "2.10.0"
  val catsCore = "org.typelevel" %% "cats-core" % "2.10.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.1"
  val catsMtl = "org.typelevel" %% "cats-mtl" % "1.4.0"
  val catsParse = "org.typelevel" %% "cats-parse" % "1.1.0"
  val smithy4sCore = "com.disneystreaming.smithy4s" %% "smithy4s-core" % "0.17.19"
  val smithy4sJson = "com.disneystreaming.smithy4s" %% "smithy4s-core" % "0.17.19"
  val munitCatsEffect = "org.typelevel" %% "munit-cats-effect" % "2.0.0"
  val monocle =  "dev.optics" %% "monocle-core"  % "3.1.0"
  val monocleMacro = "dev.optics" %% "monocle-macro" % "3.1.0"
}
