import Dependencies.*
import h8io.sbt.dependencies.*

val ProjectName = "stages"
val SiteRoot = s"https://stages.h8.io/"

organization := "io.h8"
organizationName := "H8IO"
organizationHomepage := Some(uri("https://github.com/h8io/"))
homepage := Some(uri(SiteRoot))
scmInfo := Some(ScmInfo(uri(s"https://github.com/h8io/$ProjectName"), s"scm:git@github.com:h8io/$ProjectName.git"))
licenses := List(License.Apache2)
developers := List(
  Developer(id = "eshu", name = "Pavel", email = "tjano.xibalba@gmail.com", url = uri("https://github.com/eshu/")))

scalaVersion := "2.13.18"
crossScalaVersions ++= Seq("2.12.21", "3.9.0")
javacOptions ++= Seq("--release", "11")

versionScheme := Some("semver-spec")
// sbt-dynver computes the version once for the whole build, from these two in ThisBuild
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

scalacOptions ++=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, minor)) =>
      Seq("-Xsource:3", "-language:higherKinds", "--deprecation", "--feature", "--unchecked", "-Xlint:_",
        "-Xfatal-warnings", "-opt:l:inline", "-opt-warnings") ++
        (if (minor == 13) Seq("--explain-types", "--language:_", "-Wunused:_", "-Wdead-code")
         else Seq("-Ywarn-unused", "-Ywarn-dead-code", "-Ywarn-unused:-nowarn", "-Ypartial-unification"))
    case _ =>
      // Scala 2 needs the trailing `_` to eta-expand a method without an expected function type, Scala 3 does not
      Seq("-deprecation", "-feature", "-unchecked", "-Wunused:all", "-Werror",
        "-Wconf:msg=eta-expansion is unnecessary:s")
  })

coverageSummaryStmtLowThreshold := 100
coverageSummaryStmtHighThreshold := 100
coverageSummaryBranchLowThreshold := 100
coverageSummaryBranchHighThreshold := 100

libraryDependencies ++= TestBundle % Test

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
    // sbt 2 puts the packaged jar on the test classpath, and that jar is empty here
    exportJars := false,
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
    // The sources are shared, so the Scala 2 API docs cover Scala 3 as well
    crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("3.")),
    // sbt 2 moves every target under the root's target/out; npm and release.sh expect the site's inputs here
    cleanFiles += baseDirectory.value / "target",
    ScalaUnidoc / unidoc / target := baseDirectory.value / "target" / s"scala-${scalaBinaryVersion.value}" / "unidoc",
    TestScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(examples),
    TestScalaUnidoc / unidoc / unidocConfigurationFilter := inAnyConfiguration -- inConfigurations(TestKit),
    mdocIn := (ThisBuild / baseDirectory).value / "docs",
    mdocOut := baseDirectory.value / "target" / "mdoc",
    mdocVariables :=
      Map("VERSION" -> (if (isSnapshot.value) previousStableVersion.value else None).getOrElse(version.value))
  )
  .dependsOn(root)
  .enablePlugins(ScalaUnidocPlugin, MdocPlugin)
