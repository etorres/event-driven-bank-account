package es.eriktorr
package accounts.api

import accounts.api.AccountRestControllerSuite.{createTestCaseGen, deleteTestCaseGen}
import accounts.service.FakeAccountServiceFacade.{FakeAccount, FakeOperation}
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
    forAllF(createTestCaseGen): testCase =>
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

  test("should delete accounts"):
    forAllF(deleteTestCaseGen): testCase =>
      given Decoder[Unit] = Decoder.decodeUnit
      (for (result, finalState) <- runWith(
          testCase.initialState,
          Request(
            method = Method.DELETE,
            uri = Uri.unsafeFromString(s"/api/v1/accounts/${testCase.request}"),
          ),
        )
      yield (result, finalState)).map { case (result, finalState) =>
        assertEquals(finalState, testCase.expectedState)
        assertEquals(result, Right(testCase.expectedResponse))
      }

object AccountRestControllerSuite:
  private val createTestCaseGen = for
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

  private val deleteTestCaseGen = for
    accountId <- Gen.uuid
    operations <- Gen.listOf(Gen.choose(-1000d, 1000d).map(x => FakeOperation(x)))
    initialState = AppHttpState.empty.setAccounts(
      Map(accountId.toString -> FakeAccount(0d, operations)),
    )
    expectedState = initialState.setAccounts(Map.empty)
  yield TestCase(initialState, expectedState, accountId, ((), Status.NoContent))
