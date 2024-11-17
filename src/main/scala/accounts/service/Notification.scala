package es.eriktorr
package accounts.service

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

enum Notification:
  case AccountOpened(accountId: String)
  case BalanceUpdated(accountId: String, balance: BigDecimal)
  case AccountClosed(accountId: String)

object Notification:
  given notificationJsonDecoder: Decoder[Notification] = deriveDecoder[Notification]

  given notificationJsonEncoder: Encoder[Notification] = deriveEncoder[Notification]
