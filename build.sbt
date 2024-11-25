ThisBuild / organization := "es.eriktorr"
ThisBuild / version := "1.0.0"
ThisBuild / idePackagePrefix := Some("es.eriktorr")
Global / excludeLintKeys += idePackagePrefix

ThisBuild / scalaVersion := "3.3.4"

ThisBuild / scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-source:future", // https://github.com/oleg-py/better-monadic-for
  "-Yexplicit-nulls", // https://docs.scala-lang.org/scala3/reference/other-new-features/explicit-nulls.html
  "-Ysafe-init", // https://docs.scala-lang.org/scala3/reference/other-new-features/safe-initialization.html
  "-Wnonunit-statement",
  "-Wunused:all",
)

Global / cancelable := true
Global / fork := true
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / semanticdbEnabled := true
ThisBuild / javacOptions ++= Seq("-source", "21", "-target", "21")
ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.3.1"

lazy val MUnitFramework = new TestFramework("munit.Framework")
lazy val warts = Warts.unsafe.filter(_ != Wart.DefaultArguments)

Compile / doc / sources := Seq()
Compile / compile / wartremoverErrors ++= warts
Test / compile / wartremoverErrors ++= warts
Test / testFrameworks += MUnitFramework
Test / testOptions += Tests.Argument(MUnitFramework, "--exclude-tags=online")

addCommandAlias(
  "check",
  "; undeclaredCompileDependenciesTest; unusedCompileDependenciesTest; scalafixAll; scalafmtSbtCheck; scalafmtCheckAll",
)

Test / envVars := Map(
  "SBT_TEST_ENV_VARS" -> "true",
  "BANK_ACCOUNT_HEALTH_LIVENESS_PATH" -> "/liveness-path",
  "BANK_ACCOUNT_HEALTH_READINESS_PATH" -> "/readiness-path",
  "BANK_ACCOUNT_HTTP_HOST" -> "localhost",
  "BANK_ACCOUNT_HTTP_MAX_ACTIVE_REQUESTS" -> "1024",
  "BANK_ACCOUNT_HTTP_PORT" -> "8000",
  "BANK_ACCOUNT_HTTP_TIMEOUT" -> "11s",
  "BANK_ACCOUNT_JDBC_CONNECTIONS" -> "2:4",
  "BANK_ACCOUNT_JDBC_CONNECT_URL" -> "jdbc:postgres://localhost:3306/backend",
  "BANK_ACCOUNT_JDBC_PASSWORD" -> "jdbc_password",
  "BANK_ACCOUNT_JDBC_USERNAME" -> "jdbc_username",
)

lazy val root = (project in file("."))
  .settings(
    name := "bank-account",
    Universal / maintainer := "https://eriktorr.es",
    Compile / mainClass := Some("es.eriktorr.Application"),
    libraryDependencies ++= Seq(
      "dev.optics" %% "monocle-core" % "3.3.0",
      "co.fs2" %% "fs2-core" % "3.11.0",
      "com.comcast" %% "ip4s-core" % "3.6.0",
      "com.lihaoyi" %% "os-lib" % "0.11.3" % Test,
      "com.lmax" % "disruptor" % "3.4.4" % Runtime,
      "com.monovore" %% "decline" % "2.4.1",
      "com.monovore" %% "decline-effect" % "2.4.1",
      "com.zaxxer" % "HikariCP" % "6.2.0" exclude ("org.slf4j", "slf4j-api"),
      "dev.hnaderi" %% "edomata-backend" % "0.12.4",
      "dev.hnaderi" %% "edomata-core" % "0.12.4",
      "dev.hnaderi" %% "edomata-doobie" % "0.12.4",
      "dev.hnaderi" %% "edomata-doobie-circe" % "0.12.4",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10" % Test,
      "co.fs2" %% "fs2-io" % "3.11.0",
      "io.github.iltotore" %% "iron" % "2.6.0",
      "io.github.iltotore" %% "iron-cats" % "2.6.0",
      "io.github.iltotore" %% "iron-decline" % "2.6.0",
      "io.prometheus" % "simpleclient" % "0.16.0",
      "org.apache.logging.log4j" % "log4j-core" % "2.24.1" % Runtime,
      "org.apache.logging.log4j" % "log4j-layout-template-json" % "2.24.1" % Runtime,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.24.1" % Runtime,
      "org.http4s" %% "http4s-circe" % "0.23.29",
      "org.http4s" %% "http4s-core" % "0.23.29",
      "org.http4s" %% "http4s-dsl" % "0.23.29",
      "org.http4s" %% "http4s-ember-server" % "0.23.29",
      "org.http4s" %% "http4s-prometheus-metrics" % "0.25.0",
      "org.http4s" %% "http4s-server" % "0.23.29",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-free" % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC5",
      "org.typelevel" %% "case-insensitive" % "1.4.2",
      "org.typelevel" %% "cats-collections-core" % "0.9.9",
      "org.typelevel" %% "cats-core" % "2.12.0",
      "org.typelevel" %% "cats-effect" % "3.5.5",
      "org.typelevel" %% "cats-effect-kernel" % "3.5.5",
      "org.typelevel" %% "cats-effect-std" % "3.5.5",
      "org.typelevel" %% "cats-kernel" % "2.12.0",
      "org.typelevel" %% "log4cats-core" % "2.7.0",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "org.typelevel" %% "vault" % "3.6.0",
      "com.47deg" %% "scalacheck-toolbox-datetime" % "0.7.0" % Test,
      "io.chrisdavenport" %% "cats-scalacheck" % "0.3.2" % Test,
      "org.scalameta" %% "munit" % "1.0.2" % Test,
      "org.scalameta" %% "munit-scalacheck" % "1.0.0" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test,
      "org.typelevel" %% "scalacheck-effect" % "1.0.4" % Test,
      "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4" % Test,
    ),
  )
  .enablePlugins(JavaAppPackaging)
