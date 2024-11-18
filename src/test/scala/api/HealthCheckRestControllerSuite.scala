package es.eriktorr
package api

import application.AppHttpSuite
import application.AppHttpSuiteRunner.{runWith, AppHttpState}
import common.application.HealthConfig

import org.http4s.{EntityDecoder, Method, Request, Status, Uri}

final class HealthCheckRestControllerSuite extends AppHttpSuite:
  test("should provide a liveliness endpoint"):
    testHealthCheckWith(HealthConfig.defaultLivenessPath, "ServiceName is live")

  test("should provide a readiness endpoint"):
    testHealthCheckWith(HealthConfig.defaultReadinessPath, "ServiceName is ready")

  private def testHealthCheckWith(route: String, expectedContent: String): Unit =
    val initialState = AppHttpState.empty
    val expectedState = initialState.copy()
    val expectedResponse = (expectedContent, Status.Ok)
    runWith(
      initialState,
      Request(method = Method.GET, uri = Uri.unsafeFromString(route)),
    )(using EntityDecoder.text).map { case (result, finalState) =>
      assertEquals(finalState, expectedState)
      assertEquals(result, Right(expectedResponse))
    }
