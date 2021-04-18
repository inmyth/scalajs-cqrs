package me.mbcu.cqrs.query.service.otp

import cats.data.EitherT
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.Config.QueryCrypto
import me.mbcu.cqrs.query.app.QueryError
import me.mbcu.cqrs.query.app.QueryError._
import me.mbcu.cqrs.query.crypto.QueryKey
import me.mbcu.cqrs.query.service.{Projection, Query}
import me.mbcu.cqrs.shared.http.{HttpPayload, Response}
import me.mbcu.cqrs.shared.util.UUID5
import me.mbcu.cqrs.shared.event.{Event, EventName, OtpToEmailRequested, OtpToSmsRequested}
import me.mbcu.cqrs.shared.jwt.{JwtContent, JwtPayload, Role}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import monix.eval.Task

import java.util.UUID

private[otp] case class OtpService(repository: Repository, crypto: QueryCrypto) extends Projection with Query {
  val key: QueryKey = crypto.key

  override def allowedRole: Option[Role] = None

  override def handle(event: Event): Task[Either[Throwable, Unit]] =
    event.name match {
      case EventName.OtpToEmailRequested =>
        val x = decode[OtpToEmailRequested.Data](event.data).toOption.get
        repository.save(OtpView(event.user, x.email, event.version, x.iat, x.key, x.otp, x.exp, x.role, x.country))

      case EventName.OtpToSmsRequested =>
        val x = decode[OtpToSmsRequested.Data](event.data).toOption.get
        repository.save(OtpView(event.user, x.phone, event.version, x.iat, x.key, x.otp, x.exp, x.role, x.country))

    }

  override def fetch(payload: HttpPayload): EitherT[Task, QueryError, String] =
    for {
      a <- EitherT(Task(decode[InDto](payload.body).fold(_ => Left(BadDto), x => Right(x))))
      b = UUID5.v5(a.primary)
      c <- EitherT(repository.get(b))
      d <- EitherT(Task.now(c match {
        case _ if c.isDefined && c.get.key == a.key && c.get.otp == a.otp => Right(c.get)
        case _                                                            => Left(ApiError("Authentication failed"))
      }))
      e = {
        val iat = System.currentTimeMillis() / 1000
        val exp = iat + 30 * 24 * 3600
        JwtPayload(
          iat = iat,
          exp = exp,
          sub = d.id,
          alias = d.id.toString,
          role = d.role,
          country = d.country
        )
      }
      f <- EitherT(key.sign(e))
      g = Response.ok(OutDto(f)).asJson.noSpaces
    } yield g

}

private[query] object OtpService {

  def apply(config: Config, crypto: QueryCrypto): OtpService =
    config.envMode match {
      case Config.Cloud => OtpService(new DynamoDbImpl()(Config.ec), crypto)
      case Config.InMem => OtpService(new InMemImpl()(Config.ec), crypto)
    }
}
