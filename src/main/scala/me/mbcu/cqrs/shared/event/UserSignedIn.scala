package me.mbcu.cqrs.shared.event

import me.mbcu.cqrs.shared.event.UserSignedIn.Data
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

import java.util.UUID

final case class UserSignedIn(
    override val id: UUID,
    override val version: Int,
    override val iat: Long,
    override val user: UUID,
    x: Data
) extends Event(
      id,
      version,
      Event.vhash(version),
      iat,
      id,
      UserSignedIn.aggregateName,
      UserSignedIn.eventName,
      x.asJson.noSpaces
    )

object UserSignedIn extends Meta {
  override val aggregateName: AggregateName = AggregateName.user

  override val eventName: EventName = EventName.UserSignedIn

  def apply(id: UUID, version: Int, iat: Long, primary: String, role: Role, country: Country): Event =
    new UserSignedIn(
      id,
      version,
      iat,
      id,
      Data(id, primary, role, country)
    )

  final case class Data(
      id: UUID,
      primary: String,
      role: Role,
      country: Country
  )

}
