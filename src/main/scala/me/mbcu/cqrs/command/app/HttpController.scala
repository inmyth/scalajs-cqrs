package me.mbcu.cqrs.command.app

import cats.data.{EitherT, Reader}
import me.mbcu.cqrs.shared.jwt.{JwtContent, Role}
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.Config.CommandCrypto
import me.mbcu.cqrs.command.app.CommandError.{ApiError, ForbiddenAccess, PathNotRecognized}
import me.mbcu.cqrs.command.crypto.Key
import me.mbcu.cqrs.command.domain.{Command, CommandBuilder}
import me.mbcu.cqrs.command.domain.org.{CreateOrg, UpdateOrg}
import me.mbcu.cqrs.command.domain.otp.RequestOtp
import me.mbcu.cqrs.command.domain.user.SignInUser
import me.mbcu.cqrs.shared.http.{AccessLevel, HttpPayload, open, secure}
import monix.eval.Task

import scala.util.matching.Regex

private[command] case class HttpController(crypto: CommandCrypto) {

  val key: Key = crypto.key

  final case class Path(commandBuilder: CommandBuilder, r: Regex, access: AccessLevel = secure)

  val patterns = List(
    Path(CreateOrg, "^.*/org/([A-Za-z]{6,15}$)".r),
    Path(UpdateOrg, "^.*/org/([A-Za-z]{6,15}$)/edit/([A-Za-z]{6,15}$)".r),
    Path(SignInUser, "^.*(/signin)(/){0,1}$".r, open),
    Path(RequestOtp, "^.*(/otp)(/){0,1}$".r, open)
  )

  def route(payload: HttpPayload): Either[CommandError, Path] =
    patterns
      .find(p =>
        payload.path match {
          case p.r(_*) => true
          case _       => false
        }
      )
      .toRight(PathNotRecognized(payload.path))

  def verifyToken(accessLevel: AccessLevel, jwtContent: Option[JwtContent]): Task[Either[CommandError, Unit]] =
    (accessLevel, jwtContent) match {
      case (me.mbcu.cqrs.shared.http.open, _) => Task.now(Right(()))
      case (me.mbcu.cqrs.shared.http.secure, Some(j)) =>
        key.verify(j).map {
          case Left(_)  => Left(ApiError("Unable to decrypt token"))
          case Right(_) => Right(())
        }
      case _ => Task(Left(ForbiddenAccess))
    }

  def checkTokenExpiration(exp: Long): Either[ApiError, Unit] =
    if (System.currentTimeMillis() / 1000 > exp) Left(ApiError("Token expired")) else Right(())

  def checkRoleAllowed(jwtRole: Role, allowedRole: Option[Role]): Boolean =
    allowedRole match {
      case Some(value) => value == jwtRole
      case None        => true
    }

  def buildCommand(a: HttpPayload): EitherT[Task, CommandError, Command] =
    for {
      b <- EitherT(Task(route(a)))
      _ <- EitherT(verifyToken(b.access, a.token))
      _ <- EitherT(Task(a.token match {
        case Some(value) => checkTokenExpiration(value.jwtPayload.exp)
        case None        => Right(()).withLeft[CommandError]
      }))
      c <- EitherT(b.commandBuilder.build(a, crypto))
      _ <- EitherT(Task(a.token match {
        case Some(value) =>
          if (!checkRoleAllowed(value.jwtPayload.role, c.allowPermission))
            Left(ApiError("Command does not allow bearer's role"))
          else Right(())
        case None => Right(()).withLeft[CommandError]
      }))
    } yield c

}

private[command] object HttpController {

  def fromConfig: Reader[Config, HttpController] =
    for {
      crypto <- CommandCrypto.fromConfig
    } yield HttpController(crypto)

}
