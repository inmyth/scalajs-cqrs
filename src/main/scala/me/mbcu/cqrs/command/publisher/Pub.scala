package me.mbcu.cqrs.command.publisher

import me.mbcu.cqrs.Config
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.UnableToPublishToSNS
import me.mbcu.cqrs.shared.event.Event
import facade.amazonaws.services.sns.{MessageAttributeValue, PublishInput, SNS}
import io.circe.syntax.EncoderOps
import monix.eval.Task
import monix.execution.Scheduler
import io.circe.generic.auto._

import scala.scalajs.js

abstract class Pub(val scheduler: Scheduler, val config: Config) {

  def send(
      message: String,
      subject: String,
      attrs: Map[String, String],
      group: String = "event"
  ): Task[Either[CommandError, Unit]]

  def send(event: Event): Task[Either[CommandError, Unit]] =
    send(
      event.asJson.noSpaces,
      s"${event.name.entryName}_${event.id.toString}",
      Map("event-name" -> event.name.entryName),
      event.id.toString
    )
}

object Pub {

  def fakeBroker(scheduler: Scheduler, config: Config)    = new FakeBrokerImpl(config)(scheduler)
  def sns(scheduler: Scheduler, config: Config, sns: SNS) = new SnsImpl(config, sns)(scheduler)
}

class FakeBrokerImpl(config: Config)(implicit scheduler: Scheduler) extends Pub(scheduler, config) {
  override def send(
      message: String,
      subject: String,
      attrs: Map[String, String],
      group: String
  ): Task[Either[CommandError, Unit]] = Task(Right(()))
}

class SnsImpl(config: Config, sns: SNS)(implicit scheduler: Scheduler) extends Pub(scheduler, config) {
  override def send(
      message: String,
      subject: String,
      attrs: Map[String, String],
      group: String
  ): Task[Either[CommandError, Unit]] =
    Task
      .fromFuture {
        sns
          .publish(
            PublishInput(
              Message = message,
              MessageAttributes = js.Dictionary(
                attrs
                  .map(p => p._1 -> MessageAttributeValue(DataType = "String", js.undefined, StringValue = p._2))
                  .toSeq: _*

                //                  "att1" -> MessageAttributeValue(DataType = "string", js.undefined, StringValue = "blabla"),
                //                  "att1" -> MessageAttributeValue(DataType = "string", js.undefined, StringValue = "blabla"),
              ),
              MessageGroupId = "event",
              MessageStructure = js.undefined,
              PhoneNumber = js.undefined,
              Subject = subject,
              TargetArn = js.undefined,
              TopicArn = config.snsTopic
            )
          )
          .promise()
          .toFuture
      }
      .map(p => Right(()))
      .onErrorHandle(_ => Left(UnableToPublishToSNS))
}
