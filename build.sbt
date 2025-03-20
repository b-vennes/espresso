import smithy4s.codegen.Smithy4sCodegenPlugin
import LibraryDependencies._

ThisBuild / scalaVersion := "3.6.4"

ThisBuild / semanticdbEnabled := true

ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / scalacOptions ++= Seq(
  "-Xkind-projector:underscores",
  "-Wunused:imports"
)

val core = project
  .settings(
    libraryDependencies ++= Seq(
      catsFree,
      catsCore,
      catsEffect,
      catsMtl,
      munitCatsEffect % Test
    )
  )

val example = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      catsMtl,
      catsParse,
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      monocle,
      monocleMacro,
      munitCatsEffect % Test
    )
  )
  .dependsOn(core)

lazy val root = project
  .in(file("."))
  .settings(
    name := "espresso"
  )
  .aggregate(core, example)
