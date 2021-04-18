package me.mbcu.cqrs.shared.event

import me.mbcu.cqrs.shared.event.OtpToSmsRequested.Data
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

import java.util.UUID

final case class OtpToSmsRequested(
    override val id: UUID,
    override val version: Int,
    val key: UUID,
    override val iat: Long,
    override val user: UUID,
    x: Data
) extends Event(
      id,
      version,
      key.toString,
      iat,
      user,
      OtpToSmsRequested.aggregateName,
      OtpToSmsRequested.eventName,
      x.asJson.noSpaces
    )

object OtpToSmsRequested extends Meta {

  def apply(
      id: UUID,
      phone: String,
      version: Int,
      iat: Long,
      key: UUID,
      otp: String,
      exp: Long,
      role: Role,
      country: Country
  ): Event =
    OtpToSmsRequested(id, version, key, iat, id, Data(phone, iat, key, otp, exp, role, country))

  override val aggregateName: AggregateName = AggregateName.otp

  override val eventName: EventName = EventName.OtpToSmsRequested

  final case class Data(phone: String, iat: Long, key: UUID, otp: String, exp: Long, role: Role, country: Country)

}
