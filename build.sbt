ThisBuild / organization := "com.voicetel"
ThisBuild / organizationName := "VoiceTel Communications"
ThisBuild / organizationHomepage := Some(url("https://voicetel.com"))
ThisBuild / version := "2.2.10"
ThisBuild / scalaVersion := "3.8.4"

ThisBuild / homepage := Some(url("https://github.com/voicetel/scala-sdk"))
ThisBuild / licenses := List("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/voicetel/scala-sdk"),
    "scm:git@github.com:voicetel/scala-sdk.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "voicetel",
    name = "VoiceTel",
    email = "support@voicetel.com",
    url = url("https://voicetel.com")
  )
)
ThisBuild / description := "Official Scala SDK for the VoiceTel REST API (v2.2.10)."

val sttpVersion  = "3.10.1"
val circeVersion = "0.14.10"
val munitVersion = "1.3.4"

lazy val root = (project in file("."))
  .settings(
    name := "voicetel-sdk",
    Compile / scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wunused:all"
    ),
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core"             % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe"            % sttpVersion,
      "io.circe"                      %% "circe-core"       % circeVersion,
      "io.circe"                      %% "circe-generic"    % circeVersion,
      "io.circe"                      %% "circe-parser"     % circeVersion,
      "org.scalameta"                 %% "munit"            % munitVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Test / parallelExecution := false,
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false }
  )
