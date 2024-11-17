package es.eriktorr
package accounts.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

enum Event:
  case Opened
  case Deposited(amount: BigDecimal)
  case Withdrawn(amount: BigDecimal)
  case Closed

object Event:
  given eventJsonDecoder: Decoder[Event] = deriveDecoder[Event]

  given eventJsonEncoder: Encoder[Event] = deriveEncoder[Event]
