package me.mbcu.cqrs.query.app

import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import me.mbcu.cqrs.query.service.org.OrgService
import me.mbcu.cqrs.shared.event.OrgCreated
import net.exoego.facade.aws_lambda.{SQSRecord, SQSRecordAttributes}
import org.scalatest.flatspec.AsyncFlatSpec
import me.mbcu.cqrs.{Config, TestUtil}

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.Dictionary

class ProjectionAppTest extends AsyncFlatSpec {
  implicit val ec                        = Config.ec
  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global // NEEDED !

  val id         = UUID.randomUUID()
  val orgCreated = OrgCreated(id, "muco", "jakarta", None)

  case class Body(Message: String)

  def createSqsRecord(body: String) =
    SQSRecord(
      attributes = SQSRecordAttributes(
        ApproximateFirstReceiveTimestamp = "",
        ApproximateReceiveCount = "",
        SenderId = "aa",
        SentTimestamp = "",
        AWSTraceHeader = js.undefined,
        MessageDeduplicationId = js.undefined,
        MessageGroupId = js.undefined,
        SequenceNumber = "1"
      ),
      awsRegion = "xxx",
      body = body,
      eventSource = "sns",
      eventSourceARN = "xxx",
      md5OfBody = "xxx",
      messageAttributes = Dictionary(
      ),
      messageId = "xxx",
      receiptHandle = "a"
    )

  val sqsRecord = createSqsRecord(Body(orgCreated.asJson.noSpaces).asJson.noSpaces)

  val config     = Config.load()
  val controller = ProjectionApp.fromConfig(config)

  behavior of "Projection App with OrgCreated event"

  it should "ok converting correct SQSRecord to event" in {
    val x = controller.recordToEvent(sqsRecord)
    val y = controller.recordToEvent(createSqsRecord("aaa"))
    assert(x.isRight)
    assert(x.toOption.get.id === id)
    assert(y.isLeft)
  }

  it should "ok matching event with the right handler" in {
    val x = controller.recordToEvent(sqsRecord).toOption.get
    val y = controller.route(x)
    assert(y.isInstanceOf[OrgService])
  }

  it should "ok handling/persisting projection event" in {
    controller.process(sqsRecord).runToFuture.map(p => assert(p.isRight))
  }

}
