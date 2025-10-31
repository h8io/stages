import Dependencies.*
import h8io.sbt.dependencies.*

ThisBuild / organization := "io.h8"
ThisBuild / organizationName := "H8IO"
ThisBuild / organizationHomepage := Some(url("https://github.com/h8io/"))
ThisBuild / homepage := Some(url("https://github.com/h8io/stages"))

ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / developers := List(
  Developer(
    id = "eshu",
    name = "Pavel",
    email = "tjano.xibalba@gmail.com",
    url = url("https://github.com/eshu/")))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/h8io/sbt-scoverage-summary"),
    "scm:git@github.com:h8io/sbt-scoverage-summary.git"))

ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

ThisBuild / scalaVersion := "2.13.17"
ThisBuild / crossScalaVersions += "2.12.20"

ThisBuild / scalacOptions ++=
  Seq("-Xsource:3", "-language:higherKinds", "--deprecation", "--feature", "--unchecked", "-Xlint:_",
    "-Xfatal-warnings", "-opt:l:inline", "-opt-warnings")

ThisBuild / scalacOptions ++=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("--explain-types", "--language:_", "-Wunused:_", "-Wdead-code")
    case Some((2, 12)) => Seq("-Ywarn-unused", "-Ywarn-dead-code", "-Ywarn-unused:-nowarn", "-Ypartial-unification")
    case _ => Nil
  })

ThisBuild / javacOptions ++= Seq("-target", "8")

ThisBuild / libraryDependencies ++= TestBundle % Test

val core = (project in file("core")).settings(
  name := "stages-core",
  libraryDependencies ++= TestBundle % TestKitClassifierConfiguration
).enablePlugins(TestKitClassifierPlugin)

val std =
  (project in file("std")).settings(name := "stages-std", libraryDependencies ++= Cats % CatsClassifierConfiguration)
    .dependsOn(core, core % "test->testkit").enablePlugins(CatsClassifierPlugin)

val examples = (project in file("examples")).settings(
  name := "stages-examples",
  publish / skip := true,
  publishLocal / skip := true,
  Compile / packageBin / mappings := Nil,
  Compile / packageDoc / mappings := Nil,
  Compile / packageSrc / mappings := Nil,
  Compile / doc / skip := true
).dependsOn(core, std, core % "test->testkit")

val root = (project in file("."))
  .settings(name := "stages", libraryDependencies += ((std / projectID).value % "compile").classifier("cats"))
  .dependsOn(core, std)
  .aggregate(core, std, examples)
  .enablePlugins(ScoverageSummaryPlugin)
