package me.mbcu.cqrs.query.service.otp

import me.mbcu.cqrs.shared.http.Response.Salted
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

private[otp] case class OtpView(
    id: UUID,
    primary: String,
    version: Int,
    iat: Long,
    key: UUID,
    otp: String,
    exp: Long,
    role: Role,
    country: Country
)

private[otp] case class OutDto(token: String)

private[otp] case class InDto(primary: String, key: UUID, otp: String, country: Country, role: Role)
