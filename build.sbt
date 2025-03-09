import smithy4s.codegen.Smithy4sCodegenPlugin
import LibraryDependencies._

ThisBuild / scalaVersion := "3.6.4"

ThisBuild / scalacOptions ++= Seq(
  "-Xkind-projector:underscores"
)

val core = project
  .settings(
    libraryDependencies ++= Seq(
      catsFree,
      catsCore,
      catsEffect,
      catsMtl,
      munit % Test
    )
  )

val example = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      catsMtl,
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    )
  )
  .dependsOn(core)

lazy val root = project
  .in(file("."))
  .settings(
    name := "espresso"
  )
  .aggregate(core, example)
