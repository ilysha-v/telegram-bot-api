val akkaVersion = "2.5.6"
val akkaHttpVersion = "10.0.11"

lazy val clientDeps = libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.github.pureconfig" %% "pureconfig" % "0.7.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.mockito" % "mockito-core" % "2.13.0" % Test
)

lazy val compilerOptions = scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  releaseVersionBump := sbtrelease.Version.Bump.Minor,
  clientDeps
)

lazy val client = (project in file("telegram-bot-api"))
  .settings(commonSettings)

lazy val example = (project in file("example"))
  .dependsOn(client)
  .settings(commonSettings)

lazy val root = (project in file(".")).aggregate(client, example)
