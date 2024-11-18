package es.eriktorr
package application

import common.Secret
import common.application.HealthConfig.{LivenessPath, ReadinessPath}
import common.application.HttpServerConfig.MaxActiveRequests
import common.application.JdbcConfig.{ConnectUrl, Password, Username}
import common.application.{HealthConfig, HttpServerConfig, JdbcConfig}

import cats.collections.Range
import cats.implicits.catsSyntaxEitherId
import com.comcast.ip4s.{host, ip, port}
import com.monovore.decline.{Command, Help}
import io.github.iltotore.iron.*
import munit.FunSuite

import scala.concurrent.duration.DurationInt
import scala.util.Properties

final class AppConfigSuite extends FunSuite:
  test("should load configuration from environment variables"):
    assume(Properties.envOrNone("SBT_TEST_ENV_VARS").nonEmpty, "this test runs only on sbt")
    assertEquals(
      Command(name = "name", header = "header")(AppConfig.opts)
        .parse(List.empty, sys.env),
      AppConfig(
        HealthConfig(
          LivenessPath("/liveness-path"),
          ReadinessPath("/readiness-path"),
        ),
        HttpServerConfig(
          host"localhost",
          MaxActiveRequests(1024L),
          port"8000",
          11.seconds,
        ),
        JdbcConfig.postgres(
          Range(2, 4),
          ConnectUrl("jdbc:postgres://localhost:3306/backend"),
          Secret(Password.applyUnsafe("jdbc_password")),
          Username.applyUnsafe("jdbc_username"),
        ),
      ).asRight[Help],
    )

  test("should find the local IP address"):
    assertNotEquals(AppConfig.localIpAddress, ip"127.0.0.1")
