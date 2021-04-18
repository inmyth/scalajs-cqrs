package me.mbcu.cqrs.query.service.org

import cats.data.EitherT
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.query.app.QueryError
import me.mbcu.cqrs.query.service.{Projection, Query}
import me.mbcu.cqrs.shared.http.{HttpPayload, Response}
import me.mbcu.cqrs.shared.event.{Event, EventName, OrgCreated, OrgUpdated}
import me.mbcu.cqrs.shared.jwt.Role
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import monix.eval.Task
import monix.execution.Scheduler

case class OrgService(repository: Repository) extends Projection with Query {
  implicit val ec: Scheduler = Config.ec

  override def allowedRole: Option[Role] = Some(Role.worker)

  override def handle(event: Event): Task[Either[Throwable, Unit]] =
    event.name match {

      case EventName.OrgCreated =>
        val x = decode[OrgCreated.Data](event.data).toOption.get
        repository.save(OrgView(event.user, event.version, event.vhash, x.name, x.location, x.description))

      case EventName.OrgUpdated =>
        val x = decode[OrgUpdated.Data](event.data).toOption.get
        repository.save(OrgView(event.user, event.version, event.vhash, x.name, x.location, x.description))

    }

  override def fetch(payload: HttpPayload): EitherT[Task, QueryError, String] =
    for {
      a <- EitherT(repository.get(payload.token.get.jwtPayload.sub))
      b = Response.ok[Option[OrgView]](a)
      c = b.asJson.noSpaces
    } yield c

}

object OrgService {

  def apply(config: Config): OrgService =
    config.envMode match {
      case Config.Cloud => OrgService(new DynamoDbImpl()(Config.ec))
      case Config.InMem => OrgService(new InMemImpl()(Config.ec))
    }

}
