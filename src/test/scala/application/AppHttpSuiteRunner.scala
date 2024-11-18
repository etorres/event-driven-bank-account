package es.eriktorr
package application

import accounts.service.FakeAccountServiceFacade
import accounts.service.FakeAccountServiceFacade.{AccountServiceFacadeState, FakeAccount}
import common.api.FakeHealthService.HealthServiceState
import common.api.{FakeHealthService, FakeMetricsService}
import spec.FakeUUIDGen
import spec.FakeUUIDGen.UUIDGenState

import cats.effect.{IO, Ref}
import io.github.iltotore.iron.constraint.string.ValidUUID
import io.github.iltotore.iron.refineOption
import org.http4s.server.middleware.RequestId
import org.http4s.{EntityDecoder, Request, Status}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

object AppHttpSuiteRunner:
  final case class AppHttpState(
      accountServiceFacadeState: AccountServiceFacadeState,
      healthServiceState: HealthServiceState,
      uuidGenState: UUIDGenState,
  ):
    def clearUuids(): AppHttpState = copy(uuidGenState = UUIDGenState.empty)

    def setAccounts(accounts: Map[String, FakeAccount]): AppHttpState =
      copy(accountServiceFacadeState = accountServiceFacadeState.set(accounts))

    def setUuids(uuids: List[UUID]): AppHttpState = copy(uuidGenState = uuidGenState.set(uuids))

  object AppHttpState:
    val empty: AppHttpState = AppHttpState(
      AccountServiceFacadeState.empty,
      HealthServiceState.unready,
      UUIDGenState.empty,
    )

  def runWith[A](initialState: AppHttpState, request: Request[IO])(using
      entityDecoder: EntityDecoder[IO, A],
  ): IO[(Either[Throwable, (A, Status)], AppHttpState)] = for
    accountServiceFacadeStateRef <- Ref.of[IO, AccountServiceFacadeState](
      initialState.accountServiceFacadeState,
    )
    healthServiceStateRef <- Ref.of[IO, HealthServiceState](initialState.healthServiceState)
    uuidGenStateRef <- Ref.of[IO, UUIDGenState](initialState.uuidGenState)
    accountServiceFacade = FakeAccountServiceFacade(accountServiceFacadeStateRef)(using
      FakeUUIDGen(uuidGenStateRef),
    )
    healthService = FakeHealthService(healthServiceStateRef)
    metricsService = FakeMetricsService()
    appHttp =
      given SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
      AppHttp(accountServiceFacade, healthService, metricsService).httpApp
    result <- (for
      response <- appHttp.run(request)
      status = response.status
      body <- status match
        case Status.Ok => response.as[A]
        case other =>
          IO.raiseError(IllegalStateException(s"Unexpected response status: ${other.code}"))
      _ <- IO.fromOption(for
        requestId <- response.attributes.lookup(RequestId.requestIdAttrKey)
        _ <- requestId.refineOption[ValidUUID]
      yield ())(IllegalStateException("Request Id not found"))
    yield (body, status)).attempt
    finalAccountServiceFacadeState <- accountServiceFacadeStateRef.get
    finalHealthServiceState <- healthServiceStateRef.get
    finalUuidGenState <- uuidGenStateRef.get
    finalState = initialState.copy(
      accountServiceFacadeState = finalAccountServiceFacadeState,
      healthServiceState = finalHealthServiceState,
      uuidGenState = finalUuidGenState,
    )
    _ = result match
      case Left(error) => error.printStackTrace()
      case _ => ()
  yield (result, finalState)
