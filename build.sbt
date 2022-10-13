ThisBuild / scalaVersion         := "3.2.0"
ThisBuild / organization         := "com.alterationx10"
ThisBuild / version              := Versioning.versionFromTag
ThisBuild / publishMavenStyle    := true
ThisBuild / versionScheme        := Some("early-semver")
ThisBuild / publishTo            := Some(
  "Cloudsmith API" at "https://maven.cloudsmith.io/alterationx10/bson-codecs/"
)
ThisBuild / pomIncludeRepository := { x => false }
ThisBuild / credentials += Credentials(
  "Cloudsmith API",                                      // realm
  "maven.cloudsmith.io",                                 // host
  sys.env.getOrElse("CLOUDSMITH_USER", "alterationx10"), // user
  sys.env.getOrElse("CLOUDSMITH_TOKEN", "abc123")        // password
)
ThisBuild / fork                 := true
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
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Defaults.itSettings
  )
  .configs(IntegrationTest)

addCommandAlias("fmt", "all root/scalafmtSbt root/scalafmtAll")
addCommandAlias("fmtCheck", "all root/scalafmtSbtCheck root/scalafmtCheckAll")
