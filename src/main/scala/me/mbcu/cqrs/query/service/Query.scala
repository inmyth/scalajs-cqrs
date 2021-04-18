package me.mbcu.cqrs.query.service

import cats.data.EitherT
import me.mbcu.cqrs.Config.QueryCrypto
import me.mbcu.cqrs.query.app.QueryError
import me.mbcu.cqrs.shared.http.HttpPayload
import me.mbcu.cqrs.shared.jwt.Role
import monix.eval.Task

trait Query {

  def allowedRole: Option[Role]

  def fetch(payload: HttpPayload): EitherT[Task, QueryError, String]

}
