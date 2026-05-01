import Dependencies.*
import h8io.sbt.dependencies.*

val ProjectName = "stages"
val SiteRoot = s"https://stages.h8.io/"

inThisBuild(
  List(
    organization := "io.h8",
    organizationName := "H8IO",
    organizationHomepage := Some(url("https://github.com/h8io/")),
    homepage := Some(url(SiteRoot)),
    scmInfo := Some(ScmInfo(url(s"https://github.com/h8io/$ProjectName"), s"scm:git@github.com:h8io/$ProjectName.git")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    developers := List(
      Developer(
        id = "eshu",
        name = "Pavel",
        email = "tjano.xibalba@gmail.com",
        url = url("https://github.com/eshu/")))
  ))

ThisBuild / scalaVersion := "2.13.18"
ThisBuild / crossScalaVersions += "2.12.21"
ThisBuild / javacOptions ++= Seq("-target", "8")

ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

ThisBuild / scalacOptions ++=
  Seq("-Xsource:3", "-language:higherKinds", "--deprecation", "--feature", "--unchecked", "-Xlint:_",
    "-Xfatal-warnings", "-opt:l:inline", "-opt-warnings")
ThisBuild / scalacOptions ++=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("--explain-types", "--language:_", "-Wunused:_", "-Wdead-code")
    case Some((2, 12)) => Seq("-Ywarn-unused", "-Ywarn-dead-code", "-Ywarn-unused:-nowarn", "-Ypartial-unification")
    case _ => Nil
  })

ThisBuild / libraryDependencies ++= TestBundle % Test

val core = (project in file("core"))
  .settings(
    name := "stages-core",
    libraryDependencies ++= TestBundle % TestKit)
  .enablePlugins(TestKitPlugin)

val lib = (project in file("lib"))
  .settings(
    name := "stages-lib",
    libraryDependencies ++= TestBundle % TestKit)
  .enablePlugins(TestKitPlugin)
  .dependsOn(core, core % "test->testkit")

val cats = (project in file("cats"))
  .settings(name := "stages-cats", libraryDependencies ++= Cats)
  .dependsOn(core, core % "test->testkit", lib)

val examples = (project in file("examples"))
  .settings(
    name := "stages-examples",
    publish / skip := true,
    publishLocal / skip := true,
    Compile / packageBin / mappings := Nil,
    Compile / packageDoc / mappings := Nil,
    Compile / packageSrc / mappings := Nil,
    Compile / doc / skip := true
  )
  .dependsOn(core, core % "test->testkit", lib)

val root = (project in file("."))
  .settings(name := ProjectName)
  .dependsOn(core, lib, cats)
  .aggregate(core, lib, cats, examples)
  .enablePlugins(ScoverageSummaryPlugin)

val pages = (project in file("pages"))
  .settings(
    name := "stages-pages",
    publish / skip := true,
    publishLocal / skip := true,
    TestScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(examples),
    TestScalaUnidoc / unidoc / unidocConfigurationFilter := inAnyConfiguration -- inConfigurations(TestKit),
    tlSiteApiUrl := Some(url(s"${SiteRoot}api/scala-2.13/"))
  )
  .dependsOn(root)
  .aggregate(core, lib, cats)
  .enablePlugins(ScalaUnidocPlugin, TypelevelSitePlugin)
