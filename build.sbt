ThisBuild / scalaVersion := "3.2.0"
ThisBuild / organization := "com.alterationx10"
ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:implicitConversions"
)

val zioVersion: String = "2.0.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "bson-codecs",
    libraryDependencies ++= Seq(
      "org.mongodb" % "bson"         % "4.7.1",
      "dev.zio"    %% "zio-test"     % zioVersion % "it, test",
      "dev.zio"    %% "zio-test-sbt" % zioVersion % "it, test"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
