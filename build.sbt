import Dependencies.*
import h8io.sbt.dependencies.*

val ProjectName = "stages"
val SiteRoot = s"https://stages.h8.io/"

// Every branch on the Scala version lists the versions it knows and ends here, so that a new one fails the build until
// each of them has been looked at
def unsupportedScala(version: String): Nothing = throw new MessageOnlyException(s"Unsupported Scala version $version")

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

scalacOptions ++= {
  val scala2 = Seq("-Xsource:3", "-language:higherKinds", "--deprecation", "--feature", "--unchecked", "-Xlint:_",
    "-Xfatal-warnings", "-opt:l:inline", "-opt-warnings")
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => scala2 ++ Seq("--explain-types", "--language:_", "-Wunused:_", "-Wdead-code")
    case Some((2, 12)) =>
      scala2 ++ Seq("-Ywarn-unused", "-Ywarn-dead-code", "-Ywarn-unused:-nowarn", "-Ypartial-unification")
    case Some((3, 9)) =>
      Seq(
        "-deprecation",
        "-feature",
        "-unchecked",
        // -Wall, less -Wvalue-discard and -Wnonunit-statement, which the main code adds below
        "-Wunused:all",
        "-Wimplausible-patterns",
        "-Winfer-union",
        "-Wrecurse-with-default",
        "-Wshadow:all",
        "-Wsafe-init",
        "-Wtostring-interpolated",
        "-Wenum-comment-discard",
        "-Wwrong-arrow",
        "-Werror",
        "-explain",
        "-opt",
        "-Wopt:at-inline-failed-summary",
        // Scala 2 needs the trailing `_` to eta-expand a method without an expected function type, Scala 3 does not
        "-Wconf:msg=eta-expansion is unnecessary:s"
      )
    case _ => unsupportedScala(scalaVersion.value)
  }
}

// Main code only: a test statement is routinely a ScalaMock expectation or a ScalaTest assertion whose value is dropped
Compile / compile / scalacOptions ++=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) | Some((3, 9)) => Seq("-Wvalue-discard", "-Wnonunit-statement")
    case Some((2, 12)) => Seq("-Ywarn-value-discard")
    case _ => unsupportedScala(scalaVersion.value)
  })

coverageSummaryStmtLowThreshold := 100
coverageSummaryStmtHighThreshold := 100
coverageSummaryBranchLowThreshold := 100
coverageSummaryBranchHighThreshold := 100

libraryDependencies ++= TestBundle % Test

// Scala 3 scaladoc documents the members testkit traits inherit from ScalaTest and ScalaMock, and warns about every
// link in their docs that it cannot resolve
val testKitDocScalacOptions = TestKit / doc / scalacOptions ++=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) | Some((2, 12)) => Nil
    case Some((3, 9)) => Seq("-nowarn")
    case _ => unsupportedScala(scalaVersion.value)
  })

val core = (project in file("core"))
  .settings(
    name := "stages-core",
    libraryDependencies ++= TestBundle % TestKit,
    testKitDocScalacOptions)
  .enablePlugins(TestKitPlugin)

val lib = (project in file("lib"))
  .settings(
    name := "stages-lib",
    libraryDependencies ++= TestBundle % TestKit,
    testKitDocScalacOptions)
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
    crossScalaVersions := crossScalaVersions.value.filter { version =>
      CrossVersion.partialVersion(version) match {
        case Some((2, 13)) | Some((2, 12)) => true
        case Some((3, 9)) => false
        case _ => unsupportedScala(version)
      }
    },
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
