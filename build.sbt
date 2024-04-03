import scala.collection.Seq

val Scala2Version = "2.13.13"
val Scala3Version = "3.3.3"

val CatsVersion = "2.10.0"

ThisBuild / organization := "H8IO"
ThisBuild / scalaVersion := Scala2Version
ThisBuild / crossScalaVersions := List(Scala2Version, Scala3Version)
ThisBuild / scalacOptions ++= {
  (ThisBuild / scalaVersion).value match {
    case `Scala2Version` =>
      Seq(
        s"-Xsource:$Scala3Version",
        "-Wunused:_",
        "-Wdead-code",
        "-Xlint:_"
      )
    case `Scala3Version` =>
      Seq(
        "-Wunused:all",
        "-explain",
        "-print-lines"
      )
  }
}
ThisBuild / scalacOptions ++= Seq(
  "-Xsource:3",
  "--explain-types",
  "--language:higherKinds",
  "--deprecation",
  "--feature",
  "--unchecked",
  "-Xfatal-warnings")
ThisBuild / crossScalaVersions := Seq(Scala2Version, Scala3Version)

ThisBuild / libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.typelevel" %% "cats-laws" % CatsVersion % Test,
  "org.typelevel" %% "discipline-core" % "1.5.1" % Test,
  "org.typelevel" %% "discipline-scalatest" % "2.2.0" % Test,
  compilerPlugin("org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full)
)

val core = project.in(file("core")).settings(name := "stages-core")

val root = project
  .in(file("."))
  .settings(name := "stages-all")
  .aggregate(core)
