package es.eriktorr
package spec

import com.fortysevendeg.scalacheck.datetime.YearRange
import com.fortysevendeg.scalacheck.datetime.jdk8.ArbitraryJdk8.arbInstantJdk8
import org.scalacheck.Gen

import java.time.{Instant, OffsetDateTime, ZoneOffset}

object TemporalGenerators:
  import scala.language.unsafeNulls

  private given yearRange: YearRange = YearRange.between(1990, 2060)

  val instantGen: Gen[Instant] = arbInstantJdk8.arbitrary

  val offsetDateTimeGen: Gen[OffsetDateTime] =
    instantGen.map(instant => OffsetDateTime.ofInstant(instant, ZoneOffset.UTC))
