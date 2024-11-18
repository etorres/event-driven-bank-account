package es.eriktorr
package application

import common.Secret
import common.application.HealthConfig.{LivenessPath, ReadinessPath}
import common.application.HttpServerConfig.MaxActiveRequests
import common.application.JdbcConfig.{ConnectUrl, Password, Username}
import common.application.argument.HttpServerConfigArgument.given
import common.application.argument.JdbcConfigArgument.given
import common.application.argument.RangeArgument.given
import common.application.{HealthConfig, HttpServerConfig, JdbcConfig}

import cats.Show
import cats.collections.Range
import cats.implicits.{
  catsSyntaxTuple2Semigroupal,
  catsSyntaxTuple3Semigroupal,
  catsSyntaxTuple4Semigroupal,
  showInterpolator,
}
import com.comcast.ip4s.{ipv4, Host, IpAddress, Port}
import com.monovore.decline.Opts
import io.github.iltotore.iron.decline.given

import scala.concurrent.duration.FiniteDuration
import scala.util.Using

final case class AppConfig(
    healthConfig: HealthConfig,
    httpServerConfig: HttpServerConfig,
    jdbcConfig: JdbcConfig,
)

object AppConfig:
  given Show[AppConfig] = Show.show(config => show"[backend: ${config.jdbcConfig}]")

  def opts: Opts[AppConfig] =
    val healthConfig = (
      Opts
        .env[LivenessPath](
          name = "BANK_ACCOUNT_HEALTH_LIVENESS_PATH",
          help = "Set liveness path.",
        )
        .withDefault(HealthConfig.defaultLivenessPath),
      Opts
        .env[ReadinessPath](
          name = "BANK_ACCOUNT_HEALTH_READINESS_PATH",
          help = "Set readiness path.",
        )
        .withDefault(HealthConfig.defaultReadinessPath),
    ).mapN(HealthConfig.apply)

    val httpServerConfig = (
      Opts
        .env[Host](name = "BANK_ACCOUNT_HTTP_HOST", help = "Set HTTP host.")
        .withDefault(HttpServerConfig.defaultHost),
      Opts
        .env[MaxActiveRequests](
          name = "BANK_ACCOUNT_HTTP_MAX_ACTIVE_REQUESTS",
          help = "Set HTTP max active requests.",
        )
        .withDefault(HttpServerConfig.defaultMaxActiveRequests),
      Opts
        .env[Port](name = "BANK_ACCOUNT_HTTP_PORT", help = "Set HTTP port.")
        .withDefault(HttpServerConfig.defaultPort),
      Opts
        .env[FiniteDuration](name = "BANK_ACCOUNT_HTTP_TIMEOUT", help = "Set HTTP timeout.")
        .withDefault(HttpServerConfig.defaultTimeout),
    ).mapN(HttpServerConfig.apply)

    val jdbcConfig =
      (
        Opts
          .env[Range[Int]](
            name = "BANK_ACCOUNT_JDBC_CONNECTIONS",
            help = "Set JDBC Connections.",
          )
          .validate("Must be between 1 and 16")(_.overlaps(Range(1, 16)))
          .withDefault(Range(1, 3)),
        Opts.env[ConnectUrl](
          name = "BANK_ACCOUNT_JDBC_CONNECT_URL",
          help = "Set JDBC Connect URL.",
        ),
        Opts
          .env[Password](
            name = "BANK_ACCOUNT_JDBC_PASSWORD",
            help = "Set JDBC Password.",
          )
          .map(Secret.apply[Password]),
        Opts.env[Username](
          name = "BANK_ACCOUNT_JDBC_USERNAME",
          help = "Set JDBC Username.",
        ),
      ).mapN(JdbcConfig.postgres)

    (healthConfig, httpServerConfig, jdbcConfig).mapN(AppConfig.apply)

  def localIpAddress: IpAddress = Using(java.net.DatagramSocket()) { datagramSocket =>
    import scala.language.unsafeNulls
    datagramSocket.connect(java.net.InetAddress.getByName("8.8.8.8"), 12345)
    datagramSocket.getLocalAddress.getHostAddress
  }.toOption.flatMap(IpAddress.fromString).getOrElse(ipv4"127.0.0.1")
