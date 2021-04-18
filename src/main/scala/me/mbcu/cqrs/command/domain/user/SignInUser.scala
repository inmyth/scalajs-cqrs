package me.mbcu.cqrs.command.domain.user

import cats.data.EitherT
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder, KeyEncoder}
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.{BadDto, ValidationError}
import me.mbcu.cqrs.command.domain.{Command, CommandBuilder, CommandHandler}
import me.mbcu.cqrs.command.domain.user.User.Primary
import me.mbcu.cqrs.shared.http.{HttpPayload, Response}
import me.mbcu.cqrs.shared.http.Response.Salted
import me.mbcu.cqrs.shared.util.UUID5
import monix.eval.Task

import java.util.UUID

case class SignInUser(
    primary: Primary,
    role: Role,
    country: Country
) extends Command(UUID5.v5(primary.value)) {

  override val allowPermission: Option[Role] = None

  override def getCommandHandler: CommandHandler[_] = CommandHandlerImpl()

}

object SignInUser extends CommandBuilder {

  final case class Dto(primary: String, country: Country, role: Role, isAuthenticated: Boolean, salt: String)
      extends Salted

  object Dto {
    implicit val encodeUuidKey: KeyEncoder[UUID] = KeyEncoder.instance(_.toString)
    implicit val enc: Decoder[Dto]               = deriveDecoder
    implicit val dec: Encoder[Dto]               = deriveEncoder
  }

  override def build(payload: HttpPayload, crypto: Config.CommandCrypto): Task[Either[CommandError, Command]] =
    (for {
      a <- EitherT(Task(decode[Response[String]](payload.body).fold(_ => Left(BadDto), b => Right(b))))
      b <- EitherT(crypto.key.decrypt(a.data))
      c <- EitherT(Task(decode[Dto](b).fold(_ => Left(BadDto), b => Right(b))))
      _ <- EitherT(Task(if (!c.isAuthenticated) Left(new ValidationError("Authentication rejected")) else Right(())))
      e = c.country
      f = c.role
      g <- EitherT(Task(f match {
        case Role.worker => Primary.fromPhone(c.primary, e)
        case Role.org    => Primary.fromEmail(c.primary)
      }))
    } yield SignInUser(g, f, e)).value

}
