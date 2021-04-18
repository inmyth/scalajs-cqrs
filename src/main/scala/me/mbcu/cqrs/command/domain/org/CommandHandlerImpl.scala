package me.mbcu.cqrs.command.domain.org

import me.mbcu.cqrs.shared.event._
import io.circe.generic.auto._
import io.circe.parser.decode
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.InvariantError
import me.mbcu.cqrs.command.domain.{Command, CommandHandler, Projection}
import me.mbcu.cqrs.command.domain.org.Org.{Description, Location, Name}

private[org] case class CommandHandlerImpl() extends CommandHandler[Org] {

  override val esTable: EsTable = EsTable(AggregateName.org)

  override val shouldLoadAll: Boolean = false

  override val empty: Org = null

  override def apply(a: Org, event: Event): Org =
    event.name match {

      case EventName.OrgCreated =>
        val x = decode[OrgCreated.Data](event.data).toOption.get
        new Org(event.id, Name(x.name), Location(x.location), Description(x.description))

      case EventName.OrgUpdated =>
        val x = decode[OrgUpdated.Data](event.data).toOption.get
        new Org(event.id, Name(x.name), Location(x.location), Description(x.description))
    }

  override def decide(projection: Projection[Org], command: Command): Either[CommandError, Event] =
    command match {
      case CreateOrg(orgId, name, location, description) =>
        if (projection.version >= 0)
          Left(InvariantError("Org already created"))
        else
          Right(
            OrgCreated(
              orgId,
              name.value,
              location.value,
              description.value
            )
          )

      case UpdateOrg(orgId, name, location, description) =>
        if (projection.version == -1)
          Left(InvariantError("Updating non-existent org"))
        else
          Right(
            OrgUpdated(
              orgId,
              projection.version + 1,
              name.value,
              location.value,
              description.value
            )
          )

    }

}
