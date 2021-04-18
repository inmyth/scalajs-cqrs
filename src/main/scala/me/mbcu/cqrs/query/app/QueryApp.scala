package me.mbcu.cqrs.query.app

import cats.data.{EitherT, Reader}
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.Config.QueryCrypto
import me.mbcu.cqrs.query.app.QueryError._
import me.mbcu.cqrs.query.crypto.QueryKey
import me.mbcu.cqrs.query.service.Query
import me.mbcu.cqrs.query.service.otp.OtpService
import me.mbcu.cqrs.shared.http.{AccessLevel, HttpPayload, Response, open}
import me.mbcu.cqrs.shared.jwt.{JwtContent, Role}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import monix.eval.Task
import net.exoego.facade.aws_lambda.{APIGatewayProxyEvent, APIGatewayProxyResult}

import scala.util.matching.Regex

case class QueryApp(crypto: QueryCrypto) {

  val config: Config = crypto.config
  val key: QueryKey  = crypto.key

  final case class Path(query: Query, r: Regex, access: AccessLevel)

  val patterns = List(
//    Path(CreateOrg, "^.*/org/([A-Za-z]+)".r),
//    Path(UpdateOrg, "^.*/org/([A-Za-z]+)/edit/([A-Za-z]+)".r),
//    Path(SignInUser, "^.*(/authenticate)(/){0,1}$".r, open),
    Path(OtpService(config, crypto), "^.*(/authenticate)(/){0,1}$".r, open)
  )

  def route(payload: HttpPayload): Either[QueryError, Path] =
    patterns
      .find(p =>
        payload.path match {
          case p.r(_*) => true
          case _       => false
        }
      )
      .toRight(PathNotRecognized(payload.path))

  def verifyToken(accessLevel: AccessLevel, jwtContent: Option[JwtContent]): Task[Either[QueryError, Unit]] =
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

  def process(a: HttpPayload): EitherT[Task, QueryError, String] =
    for {
      b <- EitherT(Task(route(a)))
      _ <- EitherT(verifyToken(b.access, a.token))
      _ <- EitherT(Task(a.token match {
        case Some(value) => checkTokenExpiration(value.jwtPayload.exp)
        case None        => Right(()).withLeft[QueryError]
      }))
      c = b.query
      _ <- EitherT(Task(a.token match {
        case Some(value) =>
          if (!checkRoleAllowed(value.jwtPayload.role, c.allowedRole))
            Left(ApiError("Query does not allow bearer's role"))
          else Right(())
        case None => Right(()).withLeft[QueryError]
      }))
      d <- c.fetch(a)
    } yield d

}

object QueryApp {
  val fromConfig: Reader[Config, QueryApp] = for {
    a <- QueryCrypto.fromConfig
  } yield QueryApp(a)

  def start(app: QueryApp, apiEvent: APIGatewayProxyEvent): Task[APIGatewayProxyResult] =
    app
      .process(HttpPayload.from(apiEvent))
      .value
      .map(response)

  val response: Either[QueryError, String] => APIGatewayProxyResult = {
    case Left(e) =>
      if (e.errorLevel == Fatal) println(e.messageToLog) // THE ONLY SIDE EFFECT
      val code = if (e.errorLevel == Fatal) 500 else 400
      APIGatewayProxyResult(
        body = Response.error(e.messageToUser).asJson.noSpaces,
        statusCode = code
      )

    case Right(v) =>
      APIGatewayProxyResult(
        body = v,
        statusCode = 200
      )
  }
}
