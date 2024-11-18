package es.eriktorr
package application

import AppHttpSuiteRunner.AppHttpState

import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.Status

trait AppHttpSuite extends CatsEffectSuite with ScalaCheckEffectSuite

object AppHttpSuite:
  final case class TestCase[A, B](
      initialState: AppHttpState,
      expectedState: AppHttpState,
      request: A,
      expectedResponse: (B, Status),
  )
