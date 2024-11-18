package es.eriktorr
package accounts.api

import io.circe.{Decoder, Encoder}

final case class CreateAccountResponse(accountId: String)

object CreateAccountResponse:
  given createAccountResponseJsonDecoder: Decoder[CreateAccountResponse] =
    Decoder.decodeString.map(CreateAccountResponse.apply)

  given createAccountResponseJsonEncoder: Encoder[CreateAccountResponse] =
    Encoder.encodeString.contramap(_.accountId)
