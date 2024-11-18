package es.eriktorr
package common.application.argument

import common.application.JdbcConfig.{ConnectUrl, Password, Username}

import cats.data.{NonEmptyList, ValidatedNel}
import com.monovore.decline.Argument
import io.github.iltotore.iron.cats.*

trait JdbcConfigArgument:
  given connectUrlArgument: Argument[ConnectUrl] = new Argument[ConnectUrl]:
    override def read(string: String): ValidatedNel[String, ConnectUrl] =
      ConnectUrl.validatedNel(string).leftMap(NonEmptyList.fromReducible)

    override def defaultMetavar: String = "url"

  given passwordArgument: Argument[Password] = new Argument[Password]:
    override def read(string: String): ValidatedNel[String, Password] =
      Password.validatedNel(string).leftMap(NonEmptyList.fromReducible)

    override def defaultMetavar: String = "password"

  given usernameArgument: Argument[Username] = new Argument[Username]:
    override def read(string: String): ValidatedNel[String, Username] =
      Username.validatedNel(string).leftMap(NonEmptyList.fromReducible)

    override def defaultMetavar: String = "username"

object JdbcConfigArgument extends JdbcConfigArgument
