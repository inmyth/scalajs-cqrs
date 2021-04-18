package me.mbcu.cqrs.command.app

import me.mbcu.cqrs.shared.event.EventName
import me.mbcu.cqrs.command.app.CommandError.ErrorLevel

private[command] sealed abstract class CommandError(
    val messageToUser: String,
    val messageToLog: String,
    val errorLevel: ErrorLevel
) extends Throwable

private[command] object CommandError {
  case object ConfigError           extends CommandError("System error", "Cannot load config or service", Fatal)
  case class UnknownPath(s: String) extends CommandError(s"Unknown path: $s", s"Unknown path: $s", Fatal)
  case object BadDto                extends CommandError("Cannot parse request body", "Cannot parse request body", UserInput)
  //case class TooManyOtpRequests(primary: Primary)
  //    extends ServiceError("Too many otp requests", s"Too many otp requests by ${primary.value}", UserInput)
  case object UnableToPublishToSNS      extends CommandError("System error", "Unable to publish to SNS", Fatal)
  class ValidationError(msg: String)    extends CommandError(msg, msg, UserInput)
  case class SystemError(msg: String)   extends CommandError("System error", msg, Fatal)
  case class DatabaseError(msg: String) extends CommandError("System error", msg, Fatal)
  final case class PathNotRecognized(x: String)
      extends CommandError(s"Path not recognized: $x", s"Path not recognized: $x", UserInput)
  final case class DbOptimisticLockError(x: String)
      extends CommandError(
        s"DB's event version has advanced than this event's version. Event: $x ",
        s"DB's event version has advanced than this event's version. Event: $x ",
        UserInput
      )

  case class UnhandledEvent(name: EventName)
      extends CommandError(s"Unhandled event: ${name.entryName}", s"Unhandled event: ${name.entryName}", Fatal)

  case class DecodingError(s: String) extends CommandError(s"Unable to decode message", s"Unable to decode $s", Fatal)

  case class InvariantError(msg: String) extends CommandError(msg, msg, UserInput)

  case class ApiError(msg: String) extends CommandError(msg, msg, Fatal)

  case object BadToken extends CommandError("Missing or bad token", "Missing or bad token", Fatal)

  case object ForbiddenAccess extends CommandError("Forbidden access", "Forbidden access", Fatal)

  sealed trait ErrorLevel

  case object Fatal     extends ErrorLevel
  case object UserInput extends ErrorLevel
}
