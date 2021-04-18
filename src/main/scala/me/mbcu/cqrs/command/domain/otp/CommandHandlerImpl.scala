package me.mbcu.cqrs.command.domain.otp

import me.mbcu.cqrs.shared.event._
import me.mbcu.cqrs.shared.jwt.Role
import io.circe.generic.auto._
import io.circe.parser.decode
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.domain.{Command, CommandHandler, Projection}
import me.mbcu.cqrs.command.domain.otp.Otp.Pwd
import shapeless.Lazy.apply

import java.util.UUID

private[otp] case class CommandHandlerImpl() extends CommandHandler[Otp] {
  val otpAge: Long = 30 * 3600L

  override val empty: Otp = null

  override val esTable: EsTable = EsTable(AggregateName.otp)

  override val shouldLoadAll: Boolean = false

  override def apply(a: Otp, event: Event): Otp =
    event.name match {
      case EventName.OtpToEmailRequested =>
        val x = decode[OtpToEmailRequested.Data](event.data).toOption.get
        Otp.Primary.fromEmail(x.email).toOption.get
        new Otp(
          event.id,
          Otp.Primary.fromEmail(x.email).toOption.get,
          event.iat,
          x.key,
          Pwd(x.otp),
          x.exp,
          x.role,
          x.country
        )

      case EventName.OtpToSmsRequested =>
        val x = decode[OtpToSmsRequested.Data](event.data).toOption.get
        new Otp(
          event.id,
          Otp.Primary.fromPhone(x.phone, x.country).toOption.get,
          event.iat,
          x.key,
          Pwd(x.otp),
          x.exp,
          x.role,
          x.country
        )

    }

  override def decide(projection: Projection[Otp], command: Command): Either[CommandError, Event] = {
    val iat = System.currentTimeMillis() / 1000
    val id  = command.aggregateId
    val exp = iat + otpAge
    val key = UUID.randomUUID()
    val otp = Pwd.create()

    command match {
      case RequestOtp(primary, role, country) =>
        Right(role match {
          case Role.worker =>
            OtpToSmsRequested(
              id,
              primary.value,
              projection.version + 1,
              iat,
              key,
              otp.value,
              exp,
              role,
              country
            )
          case Role.org =>
            OtpToEmailRequested(
              id,
              primary.value,
              projection.version + 1,
              iat,
              key,
              otp.value,
              exp,
              role,
              country
            )
        })
    }
  }

}
