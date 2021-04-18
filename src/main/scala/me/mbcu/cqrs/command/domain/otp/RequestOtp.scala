package me.mbcu.cqrs.command.domain.otp

import me.mbcu.cqrs.shared.jwt.{Country, Role}
import io.circe.generic.auto._
import io.circe.parser.decode
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.BadDto
import me.mbcu.cqrs.command.domain.{Command, CommandBuilder, CommandHandler}
import me.mbcu.cqrs.command.domain.otp.Otp.Primary
import me.mbcu.cqrs.shared.http.HttpPayload
import me.mbcu.cqrs.shared.util.UUID5
import monix.eval.Task

case class RequestOtp(primary: Primary, role: Role, country: Country) extends Command(UUID5.v5(primary.value)) {

  override val allowPermission: Option[Role] = None

  override def getCommandHandler: CommandHandler[_] = CommandHandlerImpl()

}

object RequestOtp extends CommandBuilder {

  final case class Dto(primary: String, country: Country, role: Role)

  override def build(payload: HttpPayload, crypto: Config.CommandCrypto): Task[Either[CommandError, Command]] =
    Task(for {
      a <- decode[Dto](payload.body) fold (_ => Left(BadDto), p => Right(p))
      b = a.role
      c = a.country
      d <- b match {
        case Role.worker => Primary.fromPhone(a.primary, c)
        case Role.org    => Primary.fromEmail(a.primary)
      }
    } yield RequestOtp(d, b, c))

}
