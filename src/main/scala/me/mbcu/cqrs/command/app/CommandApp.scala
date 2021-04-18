package me.mbcu.cqrs.command.app

import cats.data.{EitherT, Reader}
import me.mbcu.cqrs.shared.event.Event
import io.circe.syntax.EncoderOps
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.Config.{PublisherConfig, WriteRepository}
import me.mbcu.cqrs.command.app.CommandError.Fatal
import me.mbcu.cqrs.command.publisher.Pub
import me.mbcu.cqrs.command.repository.EventStore
import me.mbcu.cqrs.shared.http.{HttpPayload, Response}
import monix.eval.Task
import monix.execution.Scheduler
import net.exoego.facade.aws_lambda.{APIGatewayProxyEvent, APIGatewayProxyResult}
import io.circe.generic.auto._

case class CommandApp(
    config: Config,
    repository: WriteRepository,
    messageBroker: PublisherConfig,
    httpController: HttpController
) {
  val eventStore: EventStore = repository.eventStore
  val pub: Pub               = messageBroker.pub
  implicit val ec: Scheduler = Config.ec

  def process(apiEvent: HttpPayload): EitherT[Task, CommandError, Event] =
    for {
      a <- httpController.buildCommand(apiEvent)
      ch = a.getCommandHandler
      b <- ch.handleCommand(eventStore, a)
      _ <- EitherT(pub.send(b)) recoverWith { case _ => EitherT(ch.delete(eventStore, b)) }
    } yield b

}

object CommandApp {
  implicit val ec: Scheduler = Config.ec

  val configReader: Reader[Config, Config] = Reader(p => p)

  def fromConfig: Reader[Config, CommandApp] =
    for {
      config <- configReader
      repo   <- WriteRepository.fromConfig
      broker <- PublisherConfig.fromConfig
      httpC  <- HttpController.fromConfig
    } yield CommandApp(config, repo, broker, httpC)

  val response: Either[CommandError, Event] => APIGatewayProxyResult = {
    case Left(e) =>
      if (e.errorLevel == Fatal) println(e.messageToLog) // THE ONLY SIDE EFFECT
      val code = if (e.errorLevel == Fatal) 500 else 400
      APIGatewayProxyResult(
        body = Response.error(e.messageToUser).asJson.noSpaces,
        statusCode = code
      )

    case Right(v) =>
      APIGatewayProxyResult(
        body = Response.ok[OutDto](OutDto(v.vhash)).asJson.noSpaces,
        statusCode = 200
      )
  }

  case class OutDto(vhash: String)

  def start(app: CommandApp, apiEvent: APIGatewayProxyEvent): Task[APIGatewayProxyResult] =
    app
      .process(HttpPayload.from(apiEvent))
      .value
      .map(response)

}
