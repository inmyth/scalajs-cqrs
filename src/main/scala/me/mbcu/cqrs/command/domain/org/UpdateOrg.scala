package me.mbcu.cqrs.command.domain.org

import me.mbcu.cqrs.Config
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError._
import me.mbcu.cqrs.command.domain.org.Org.{Description, Location, Name}
import me.mbcu.cqrs.command.domain.{Command, CommandBuilder, CommandHandler}
import me.mbcu.cqrs.shared.http.HttpPayload
import me.mbcu.cqrs.shared.jwt.Role
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import monix.eval.Task

import java.util.UUID

case class UpdateOrg(orgId: UUID, name: Name, location: Location, description: Description) extends Command(orgId) {

  override val allowPermission: Option[Role] = Some(Role.org)

  override def getCommandHandler: CommandHandler[_] = CommandHandlerImpl()

}

object UpdateOrg extends CommandBuilder {

  case class Dto(name: String, location: String, description: Option[String])
  object Dto {
    implicit val dec: Decoder[Dto] = deriveDecoder

  }

  def fromDto(body: String, commanderId: UUID): Either[Throwable, UpdateOrg] =
    for {
      a <- decode[Dto](body)
      c <- Name.create(a.name)
      d <- Location.create(a.location)
      e <- Description.create(a.description)
    } yield UpdateOrg(commanderId, c, d, e)

  override def build(payload: HttpPayload, crypto: Config.CommandCrypto): Task[Either[CommandError, Command]] =
    Task(
      for {
        a <- decode[Dto](payload.body) fold (_ => Left(BadDto), p => Right(p))
        c <- Name.create(a.name)
        d <- Location.create(a.location)
        e <- Description.create(a.description)
        f <- Either.cond(payload.token.isDefined, payload.token.get, BadToken)
      } yield UpdateOrg(f.jwtPayload.sub, c, d, e)
    )

}
