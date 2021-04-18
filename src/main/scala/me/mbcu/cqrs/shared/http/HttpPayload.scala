package me.mbcu.cqrs.shared.http

import me.mbcu.cqrs.shared.jwt.JwtContent
import net.exoego.facade.aws_lambda.APIGatewayProxyEvent

import scala.scalajs.js

final case class HttpPayload(
    path: String,
    pathParameters: Map[String, String],
    body: String,
    token: Option[JwtContent]
)

object HttpPayload {

  def build(
      path: String,
      pathParameters: Map[String, String],
      body: String,
      tokenHeader: Option[String]
  ): HttpPayload =
    HttpPayload(path, pathParameters, body, tokenHeader.flatMap(JwtContent.parseToken))

  def from(event: APIGatewayProxyEvent): HttpPayload =
    HttpPayload.build(
      event.path,
      Option(event.pathParameters)
        .map(_.asInstanceOf[js.Dictionary[String]].toMap[String, String])
        .getOrElse(Map.empty[String, String]),
      event.body.asInstanceOf[String],
      event.headers.get("token")
    )

}

sealed trait AccessLevel
object open   extends AccessLevel
object secure extends AccessLevel
