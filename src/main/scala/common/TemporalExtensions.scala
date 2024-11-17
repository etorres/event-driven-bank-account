package es.eriktorr
package common

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object TemporalExtensions:
  import scala.language.unsafeNulls

  given dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  extension (value: OffsetDateTime)
    def asString(using formatter: DateTimeFormatter): String =
      formatter.format(value.toLocalDate)
