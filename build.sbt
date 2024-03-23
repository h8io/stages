import scala.collection.Seq

val Scala2Version = "2.13.13"
val Scala3Version = "3.3.3"

val root = project
  .in(file("."))
  .settings(
    name := "stages",
    organization := "H8IO",
    scalaVersion := Scala2Version,
    crossScalaVersions := List(Scala2Version, Scala3Version),
    scalacOptions ++= {
      scalaVersion.value match {
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
    },
    scalacOptions ++= Seq(
      "--explain-types",
      "--language:higherKinds",
      "--deprecation",
      "--feature",
      "--unchecked",
      "-Xfatal-warnings"),
    crossScalaVersions := Seq(Scala2Version, Scala3Version),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      "org.typelevel" %% "cats-laws" % "2.10.0" % Test,
      "org.typelevel" %% "discipline-core" % "1.5.1" % Test,
      "org.typelevel" %% "discipline-scalatest" % "2.2.0" % Test
    )
  )
