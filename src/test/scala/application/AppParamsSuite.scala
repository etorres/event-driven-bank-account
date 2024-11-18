package es.eriktorr
package application

import cats.implicits.catsSyntaxEitherId
import com.monovore.decline.{Command, Help}
import munit.FunSuite

final class AppParamsSuite extends FunSuite:
  test("should load default parameters"):
    assertEquals(
      Command(name = "name", header = "header")(AppParams.opts).parse(List.empty),
      AppParams(false).asRight[Help],
    )

  test("should load parameters from application arguments"):
    assertEquals(
      Command(name = "name", header = "header")(AppParams.opts).parse(List("-v")),
      AppParams(true).asRight[Help],
    )
