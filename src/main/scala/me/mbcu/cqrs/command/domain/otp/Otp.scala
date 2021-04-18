package me.mbcu.cqrs.command.domain.otp

import me.mbcu.cqrs.shared.jwt.{Country, Role}
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.ValidationError
import me.mbcu.cqrs.command.domain.Aggregate
import me.mbcu.cqrs.command.domain.otp.Otp.{Primary, Pwd}
import me.mbcu.cqrs.shared.util.{EmailUtil, PhoneNumber, PhoneUtil}

import java.util.UUID
import scala.util.{Failure, Success, Try}

private[otp] class Otp(
    id: UUID,
    primary: Primary,
    iat: Long,
    key: UUID,
    pwd: Pwd,
    exp: Long,
    role: Role,
    country: Country
) extends Aggregate(id)

private[otp] object Otp {

  private[otp] class Primary private (val value: String) extends AnyVal
  private[otp] object Primary {
    def fromPhone(value: String, countryCode: Country): Either[CommandError, Primary] =
      Try(PhoneUtil.parsePhoneNumberWithError(value, countryCode.entryName)) match {
        case Failure(_)     => Left(InvalidPhone)
        case Success(value) => Right(new Primary(value.asInstanceOf[PhoneNumber].number))
      }

    def fromEmail(value: String): Either[CommandError, Primary] =
      if (!EmailUtil.check(value)) Left(InvalidEmail)
      else Right(new Primary(value))
  }

  case class Pwd(value: String) extends AnyVal
  object Pwd {
    def create(): Pwd = Pwd((1 to 5).map(_ => scala.util.Random.between(0, 10)).mkString(""))
  }

  case object InvalidId    extends ValidationError("Invalid id")
  case object AliasEmpty   extends ValidationError("Empty alias")
  case object InvalidAlias extends ValidationError("Invalid alias")
  case object InvalidPhone extends ValidationError("Invalid phone number")
  case object InvalidEmail extends ValidationError("Invalid email")

}
