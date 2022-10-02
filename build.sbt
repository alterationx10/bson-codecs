ThisBuild / scalaVersion := "3.2.0"
ThisBuild / organization := "com.alterationx10"
ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:implicitConversions"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "bson-codecs",
    libraryDependencies ++= Seq(
      "org.mongodb" % "bson" % "4.7.1"
    )
  )
