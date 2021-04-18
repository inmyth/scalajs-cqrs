package me.mbcu.cqrs

import me.mbcu.cqrs.command.app.CommandApp
import me.mbcu.cqrs.query.app.{ProjectionApp, QueryApp}
import monix.eval.Task
import monix.execution.Scheduler
import net.exoego.facade.aws_lambda.{APIGatewayProxyEvent, Context, SQSEvent}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichFutureNonThenable
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportTopLevel

object Handler {
  implicit val ec: Scheduler = Config.ec

  @JSExportTopLevel(name = "handler")
  val handler: js.Function2[js.Object, Context, js.Promise[js.Object]] = { (event: js.Object, _: Context) =>
    (for {
      a <- Task(Config.load())
      b <- Task(JSON.stringify(event))
      c <- awsEvent2Service(a, b, event)
    } yield c).runToFuture.toJSPromise

  }

  def awsEvent2Service(config: Config, json: String, o: js.Object): Task[js.Object] =
    json match {

      case e if e startsWith ("{\"resource\":") =>
        val apiEvent = o.asInstanceOf[APIGatewayProxyEvent]
        (apiEvent.path, apiEvent.httpMethod) match {
          case (x, y) if "^.*(/authenticate)(/){0,1}$".r.unapplySeq(x).isDefined && y == "POST" =>
            val app = QueryApp.fromConfig(config)
            QueryApp.start(app, apiEvent)

          case (_, y) if y == "POST" || y == "PUT" =>
            val app = CommandApp.fromConfig(config)
            CommandApp.start(app, apiEvent)

          case (_, _) =>
            val app = QueryApp.fromConfig(config)
            QueryApp.start(app, apiEvent)
        }

      case e if e startsWith ("{\"Records\":") =>
        val sqsEvent = o.asInstanceOf[SQSEvent]
        val app      = ProjectionApp.fromConfig(config)
        ProjectionApp.start(app, sqsEvent)

      case _ => Task.now(js.Object)
    }

}
