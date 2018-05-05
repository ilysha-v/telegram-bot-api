import sbt.Keys.licenses

val akkaVersion = "2.5.12"
val akkaHttpVersion = "10.1.1"

lazy val clientDeps = libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
lazy val compilerOptions = scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-unchecked",
  "-feature",
  "-deprecation",
  "-Xfuture",
  "-Xlog-implicits",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  releaseVersionBump := sbtrelease.Version.Bump.Minor,
  clientDeps,
  compilerOptions
)

lazy val client = (project in file("telega-client"))
  .settings(commonSettings)
  .settings(Seq(
    bintrayRepository := "telega",
    licenses += ("GPL-3.0", url("http://opensource.org/licenses/GPL-3.0"))),
    name := "telega-client",
    organization := "com.github.ilyshav",
    publishTo := {
      val defaultDestination = publishTo.value
      if (isSnapshot.value)
        Some("OSS JFrog Snapshots" at "https://oss.jfrog.org/artifactory/oss-snapshot-local")
      else defaultDestination
    }
  )

lazy val example = (project in file("example"))
  .dependsOn(client)
  .settings(commonSettings)
  .settings(libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.7.0")
  .settings(skip in publish := true)

lazy val root = (project in file(".")).aggregate(client, example)
publish in root := publish in client




