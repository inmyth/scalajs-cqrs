package me.mbcu.cqrs.shared.event

import me.mbcu.cqrs.shared.event.OrgCreated.Data
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

import java.util.UUID

final case class OrgCreated(
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
      user,
      OrgCreated.aggregateName,
      OrgCreated.eventName,
      x.asJson.noSpaces
    )

object OrgCreated extends Meta {
  override val aggregateName: AggregateName = AggregateName.org

  override val eventName: EventName = EventName.OrgCreated

  def apply(orgId: UUID, name: String, location: String, description: Option[String]): Event =
    OrgCreated(orgId, 0, System.currentTimeMillis() / 1000, orgId, Data(name, location, description))

  final case class Data(
      name: String,
      location: String,
      description: Option[String]
  )

}
