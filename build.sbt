ThisBuild / scalaVersion := "2.13.10"

ThisBuild / version := "0.1.0"
ThisBuild / organization := "dev.ethanwu.chisel"

val chiselVersion = "3.5.6"

val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "edu.berkeley.cs" %% "chisel3" % chiselVersion,
    "edu.berkeley.cs" %% "chiseltest" % "0.5.6" % Test,

    // Property testing libs
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0" % Test
    // "com.spotify" %% "magnolify-scalacheck" % "0.6.2" % Test
  ),
  scalacOptions ++= Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature"
  ),
  scalacOptions += "-Ywarn-unused",
  addCompilerPlugin(
    "edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full
  )
)

lazy val testutil = (project in file("testutil"))
  .settings(
    name := "testutil",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chiseltest" % "0.5.6"
    )
  )
  .settings(commonSettings)

lazy val decoupledutil = (project in file("decoupledutil"))
  .settings(
    name := "decoupledutil"
  )
  .dependsOn(testutil % Test)
  .settings(commonSettings)
