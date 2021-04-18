package me.mbcu.cqrs.query.app

import me.mbcu.cqrs.query.app.QueryError.ErrorLevel

private[query] sealed abstract class QueryError(
    val messageToUser: String,
    val messageToLog: String,
    val errorLevel: ErrorLevel
) extends Throwable

private[query] object QueryError {
  case object BadDto                  extends QueryError("Incorrect input format", "Incorrect input format", Fatal)
  case class SystemError(msg: String) extends QueryError("System error", msg, Fatal)
  final case class PathNotRecognized(x: String)
      extends QueryError(s"Path not recognized: $x", s"Path not recognized: $x", UserInput)
  case object ForbiddenAccess      extends QueryError("Forbidden access", "Forbidden access", Fatal)
  case class ApiError(msg: String) extends QueryError(msg, msg, UserInput)

  case object BadJwt extends QueryError("Token cannot be verified", "Token cannot be verified", Fatal)

  sealed trait ErrorLevel

  case object Fatal     extends ErrorLevel
  case object UserInput extends ErrorLevel
}
