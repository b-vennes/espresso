import smithy4s.codegen.Smithy4sCodegenPlugin

ThisBuild / scalaVersion := "3.3.0"

val core = project
  .in(file("core"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-free" % "2.10.0",
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "cats-effect" % "3.5.1",
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % "0.17.19",
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-json" % "0.17.19",
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )

val example = project
  .in(file("example"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.18"
    )
  )
  .dependsOn(core)

lazy val root = project
  .in(file("."))
  .aggregate(core, example)
