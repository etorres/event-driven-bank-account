package es.eriktorr
package accounts.api

import accounts.api.AccountRestControllerSuite.testCaseGen
import accounts.service.FakeAccountServiceFacade.FakeAccount
import application.AppHttpSuite
import application.AppHttpSuite.TestCase
import application.AppHttpSuiteRunner.{runWith, AppHttpState}

import io.circe.Decoder
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Status, Uri}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

final class AccountRestControllerSuite extends AppHttpSuite:
  test("should create new accounts"):
    forAllF(testCaseGen): testCase =>
      given Decoder[CreateAccountResponse] = CreateAccountResponse.createAccountResponseJsonDecoder
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(method = Method.POST, uri = uri"/api/v1/accounts")
            .withEntity(testCase.request),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(finalState, testCase.expectedState)
        assertEquals(result, Right(testCase.expectedResponse))
      }

object AccountRestControllerSuite:
  private val testCaseGen = for
    accountId <- Gen.uuid
    initialState = AppHttpState.empty.setUuids(List(accountId))
    expectedState = initialState
      .clearUuids()
      .setAccounts(Map(accountId.toString -> FakeAccount(0d, List.empty)))
  yield TestCase(
    initialState,
    expectedState,
    (),
    (CreateAccountResponse(accountId.toString), Status.Ok),
  )
