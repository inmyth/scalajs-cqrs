package me.mbcu.cqrs.query.service.otp

import me.mbcu.cqrs.query.app.QueryError
import me.mbcu.cqrs.query.app.QueryError.SystemError
import me.mbcu.cqrs.query.service.Table
import me.mbcu.cqrs.shared.event.{AggregateName, QueryTable}
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import facade.amazonaws.services.dynamodb.{AttributeValue, DynamoDB, GetItemInput, PutItemInput}
import monix.eval.Task
import monix.execution.Scheduler

import java.util.UUID
import scala.collection.mutable
import scala.scalajs.js.Dictionary

private[otp] abstract class Repository(val scheduler: Scheduler) extends Table {
  /*
      id: UUID,
      email: String,
      version: Int,
      iat: Long,
      key: UUID,
      otp: String,
      exp: Long,
      role: Role,
      country: Country
   */

  override def tableName: QueryTable = QueryTable(AggregateName.otp)

  def save(v: OtpView): Task[Either[Throwable, Unit]]

  def get(id: UUID): Task[Either[QueryError, Option[OtpView]]]
}

private[otp] class InMemImpl(implicit scheduler: Scheduler) extends Repository(scheduler) {
  private val db: mutable.Map[UUID, OtpView] = mutable.Map.empty

  override def save(v: OtpView): Task[Either[Throwable, Unit]] = Task(Right(db.put(v.id, v)))

  override def get(id: UUID): Task[Either[QueryError, Option[OtpView]]] = Task(Right(db.get(id)))
}

private[otp] class DynamoDbImpl(implicit scheduler: Scheduler) extends Repository(scheduler) {
  val db = new DynamoDB()

  override def save(v: OtpView): Task[Either[Throwable, Unit]] =
    Task
      .fromFuture(
        db.putItemFuture(
          PutItemInput(
            Dictionary(
              "id"      -> AttributeValue.S(v.id.toString),
              "primary" -> AttributeValue.S(v.primary),
              "version" -> AttributeValue.NFromInt(v.version),
              "iat"     -> AttributeValue.NFromLong(v.iat),
              "key"     -> AttributeValue.S(v.key.toString),
              "otp"     -> AttributeValue.S(v.otp),
              "exp"     -> AttributeValue.NFromLong(v.exp),
              "role"    -> AttributeValue.S(v.role.entryName),
              "country" -> AttributeValue.S(v.country.entryName)
            ),
            TableName = tableName.name
          )
        )
      )
      .map(_ => Right(()))
      .onErrorHandle(e => Left(e))

  override def get(id: UUID): Task[Either[QueryError, Option[OtpView]]] =
    Task
      .fromFuture(
        db.getItemFuture(
          GetItemInput(
            Key = Dictionary(
              "id" -> AttributeValue.S(id.toString)
            ),
            TableName = tableName.name
          )
        )
      )
      .map(p =>
        Right(
          p.Item.toOption.map(q => {
            val primary = q.get("primary").get.S.get
            val version = q.get("version").get.N.get.toInt
            val iat     = q.get("iat").get.N.get.toLong
            val key     = q.get("key").get.S.get
            val otp     = q.get("otp").get.S.get
            val exp     = q.get("exp").get.N.get.toLong
            val role    = q.get("role").get.S.get
            val country = q.get("country").get.S.get
            OtpView(
              id,
              primary,
              version,
              iat,
              UUID.fromString(key),
              otp,
              exp,
              Role.withName(role),
              Country.withName(country)
            )
          })
        )
      )
      .onErrorHandle(e => Left(SystemError(e.getMessage)))
}
