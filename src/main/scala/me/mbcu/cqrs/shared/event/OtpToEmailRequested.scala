package me.mbcu.cqrs.shared.event

import me.mbcu.cqrs.shared.event.OtpToEmailRequested.Data
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

import java.util.UUID

final case class OtpToEmailRequested(
    override val id: UUID,
    override val version: Int,
    key: UUID,
    override val iat: Long,
    override val user: UUID,
    x: Data
) extends Event(
      id,
      version,
      key.toString,
      iat,
      user,
      OtpToEmailRequested.aggregateName,
      OtpToEmailRequested.eventName,
      x.asJson.noSpaces
    )

object OtpToEmailRequested extends Meta {
  override val aggregateName: AggregateName = AggregateName.otp
  override val eventName: EventName         = EventName.OtpToEmailRequested

  def apply(
      id: UUID,
      email: String,
      version: Int,
      iat: Long,
      key: UUID,
      otp: String,
      exp: Long,
      role: Role,
      country: Country
  ): Event =
    OtpToEmailRequested(id, version, key, iat, id, Data(email, iat, key, otp, exp, role, country))

  final case class Data(email: String, iat: Long, key: UUID, otp: String, exp: Long, role: Role, country: Country)

}
