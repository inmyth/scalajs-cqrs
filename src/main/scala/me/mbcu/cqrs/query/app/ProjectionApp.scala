package me.mbcu.cqrs.query.app

import cats.data.{EitherT, Reader}
import cats.implicits.toTraverseOps
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.Config.QueryCrypto
import me.mbcu.cqrs.query.app.ProjectionApp.{decodeBodyToEvent, decodeSqsRecordToRawMessage}
import me.mbcu.cqrs.query.service.Projection
import me.mbcu.cqrs.query.service.org.OrgService
import me.mbcu.cqrs.query.service.otp.OtpService
import me.mbcu.cqrs.shared.event.{Event, EventName}
import io.circe.Decoder.Result
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.{decode, parse}
import monix.eval.Task
import net.exoego.facade.aws_lambda.{SQSEvent, SQSRecord}

import scala.scalajs.js

final case class ProjectionApp(queryCrypto: QueryCrypto) {
  val config: Config = queryCrypto.config

  def route(event: Event): Projection =
    event.name match {
      case EventName.OrgCreated          => OrgService(config)
      case EventName.OrgUpdated          => OrgService(config)
      case EventName.OtpToEmailRequested => OtpService(config, queryCrypto)
      case EventName.OtpToSmsRequested   => OtpService(config, queryCrypto)
//      case EventName.UserSignedIn                       =>
    }

  def process(records: Seq[SQSRecord]): Task[js.Object] =
    for {
      a <- records.map(p => process(p)).sequence
      _ <- Task(a.foreach {
        case Left(value) => println(value.getMessage)
        case Right(_)    => ()
      })
    } yield new js.Object()

  def process(record: SQSRecord): Task[Either[Throwable, Unit]] =
    (for {
      a <- EitherT(Task(recordToEvent(record)))
      b <- handleEvent(a)
    } yield b).value

  def recordToEvent(record: SQSRecord): Either[Throwable, Event] =
    for {
      a <- decodeSqsRecordToRawMessage(record).toTry.toEither
      b <- decodeBodyToEvent(a)
    } yield b

  def handleEvent(event: Event): EitherT[Task, Throwable, Unit] =
    for {
      a <- EitherT(Task(Right(route(event)).withLeft[Throwable]))
      b <- EitherT(a.handle(event))
    } yield b

}

object ProjectionApp {

  val fromConfig: Reader[Config, ProjectionApp] = for {
    a <- QueryCrypto.fromConfig
  } yield ProjectionApp(a)

  def start(app: ProjectionApp, sqsEvent: SQSEvent): Task[js.Object] =
    app.process(sqsEvent.Records.toSeq)

  def decodeSqsRecordToRawMessage(record: SQSRecord): Result[String] = {
    val doc = parse(record.body).getOrElse(Json.Null)
    doc.hcursor.get[String]("Message") // body is in SNS format
  }

  def decodeBodyToEvent(body: String): Either[Throwable, Event] = decode[Event](body)

}
