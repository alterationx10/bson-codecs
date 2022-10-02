ThisBuild / scalaVersion := "3.2.0"

ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:implicitConversions"
)

lazy val root = project
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.mongodb" % "bson" % "4.7.1"
    )
  )
