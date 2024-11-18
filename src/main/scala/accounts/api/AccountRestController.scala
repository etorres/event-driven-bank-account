package es.eriktorr
package accounts.api

import accounts.service.AccountServiceFacade
import common.api.BaseRestController

import cats.effect.IO
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request, Response}
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class AccountRestController(accountServiceFacade: AccountServiceFacade)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController:
  override val routes: Option[HttpRoutes[IO]] = Some(
    HttpRoutes.of[IO] {
      case request @ DELETE -> Root / "accounts" / accountId =>
        (for
          _ <- accountServiceFacade.close(accountId)
          response <- NoContent()
        yield response).handleErrorWith(contextFrom(request))
      case request @ POST -> Root / "accounts" =>
        (for
          accountId <- accountServiceFacade.createAccount
          response <- Ok(CreateAccountResponse(accountId))
        yield response).handleErrorWith(contextFrom(request))
    },
  )
