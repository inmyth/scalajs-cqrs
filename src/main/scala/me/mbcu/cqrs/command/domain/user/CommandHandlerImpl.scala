package me.mbcu.cqrs.command.domain.user

import me.mbcu.cqrs.shared.event._
import me.mbcu.cqrs.shared.jwt.Role
import io.circe.generic.auto._
import io.circe.parser.decode
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.domain.{Command, CommandHandler, Projection}

private[user] case class CommandHandlerImpl() extends CommandHandler[User] {

  override val esTable: EsTable = EsTable(AggregateName.user)

  override val shouldLoadAll: Boolean = false

  override val empty: User = null

  override def apply(a: User, event: Event): User =
    event.name match {
      case EventName.UserSignedIn =>
        val x = decode[UserSignedIn.Data](event.data).toOption.get
        val p = (x.role match {
          case Role.worker => User.Primary.fromPhone(x.primary, x.country)
          case Role.org    => User.Primary.fromEmail(x.primary)
        }).toOption.get
        new User(event.id, p, x.role, x.country)
    }

  override def decide(projection: Projection[User], command: Command): Either[CommandError, Event] = {
    val iat = System.currentTimeMillis() / 1000
    command match {
      case SignInUser(primary, role, country) =>
        Right(
          UserSignedIn(
            command.aggregateId,
            projection.version + 1,
            iat,
            primary.value,
            role,
            country
          )
        )
    }
  }

}
