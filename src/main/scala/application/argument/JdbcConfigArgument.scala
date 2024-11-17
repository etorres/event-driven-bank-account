package es.eriktorr
package application.argument

import application.JdbcConfig.{ConnectUrl, Password, Username}

import cats.data.{NonEmptyList, ValidatedNel}
import com.monovore.decline.Argument

trait JdbcConfigArgument:
  given connectUrlArgument: Argument[ConnectUrl] = new Argument[ConnectUrl]:
    override def read(string: String): ValidatedNel[String, ConnectUrl] =
      ConnectUrl.from(string).leftMap(NonEmptyList.fromReducible)

    override def defaultMetavar: String = "url"

  given passwordArgument: Argument[Password] = new Argument[Password]:
    override def read(string: String): ValidatedNel[String, Password] =
      Password.from(string).leftMap(NonEmptyList.fromReducible)

    override def defaultMetavar: String = "password"

  given usernameArgument: Argument[Username] = new Argument[Username]:
    override def read(string: String): ValidatedNel[String, Username] =
      Username.from(string).leftMap(NonEmptyList.fromReducible)

    override def defaultMetavar: String = "username"

object JdbcConfigArgument extends JdbcConfigArgument
