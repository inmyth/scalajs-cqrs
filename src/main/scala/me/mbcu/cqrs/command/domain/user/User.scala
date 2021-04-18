package me.mbcu.cqrs.command.domain.user

import me.mbcu.cqrs.shared.jwt.{Country, Role}
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.ValidationError
import me.mbcu.cqrs.command.domain.Aggregate
import me.mbcu.cqrs.command.domain.user.User.Primary
import me.mbcu.cqrs.shared.util.{EmailUtil, PhoneNumber, PhoneUtil}

import java.util.UUID
import scala.util.{Failure, Success, Try}

private[user] class User(
    id: UUID,
    primary: Primary,
    role: Role,
    country: Country
) extends Aggregate(id)

private[user] object User {

  case object InvalidId    extends ValidationError("Invalid id")
  case object AliasEmpty   extends ValidationError("Empty alias")
  case object InvalidAlias extends ValidationError("Invalid alias")
  case object InvalidPhone extends ValidationError("Invalid phone number")
  case object InvalidEmail extends ValidationError("Invalid email")

  class Primary private (val value: String) extends AnyVal
  object Primary {
    def fromPhone(value: String, countryCode: Country): Either[CommandError, Primary] =
      Try(PhoneUtil.parsePhoneNumberWithError(value, countryCode.entryName)) match {
        case Failure(_)     => Left(InvalidPhone)
        case Success(value) => Right(new Primary(value.asInstanceOf[PhoneNumber].number))
      }

    def fromEmail(value: String): Either[CommandError, Primary] =
      if (!EmailUtil.check(value)) Left(InvalidEmail)
      else Right(new Primary(value))
  }

  case class Alias(value: String) extends AnyVal
  object Alias {
    private val regex = """^[a-zA-Z0-9._-]+$""".r

    def create(v: String): Either[ValidationError, Alias] =
      v match {
        case e if e.isEmpty                          => Left(AliasEmpty)
        case e if regex.matches(v) && e.length <= 30 => Right(Alias(v))
        case _                                       => Left(InvalidAlias)
      }

    def generate: Alias = Alias(UUID.randomUUID().toString.split("[-]")(0))
  }
}
