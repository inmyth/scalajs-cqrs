package me.mbcu.cqrs.query.service.org

import me.mbcu.cqrs.query.app.QueryError
import me.mbcu.cqrs.query.app.QueryError._
import me.mbcu.cqrs.query.service.Table
import me.mbcu.cqrs.shared.event.{AggregateName, QueryTable}
import facade.amazonaws.services.dynamodb.{AttributeValue, DynamoDB, GetItemInput, PutItemInput}
import monix.eval.Task
import monix.execution.Scheduler

import java.util.UUID
import scala.collection.mutable
import scala.scalajs.js.Dictionary

private[org] abstract class Repository(val scheduler: Scheduler) extends Table {

  override val tableName: QueryTable = QueryTable(AggregateName.org)

  def save(v: OrgView): Task[Either[Throwable, Unit]]

  def get(id: UUID): Task[Either[QueryError, Option[OrgView]]]

}

private[org] final class InMemImpl(implicit scheduler: Scheduler) extends Repository(scheduler) {
  private val db: mutable.Map[String, OrgView] = mutable.Map.empty

  override def save(v: OrgView): Task[Either[Throwable, Unit]] =
    Task(Right(db += v.id.toString -> v))

  override def get(id: UUID): Task[Either[SystemError, Option[OrgView]]] =
    Task(Right(db.get(id.toString)))
}

private[org] final class DynamoDbImpl(implicit scheduler: Scheduler) extends Repository(scheduler) {
  val db = new DynamoDB()

  override def save(v: OrgView): Task[Either[Throwable, Unit]] = {
    val x = Dictionary(
      "id"       -> AttributeValue.S(v.id.toString),
      "version"  -> AttributeValue.NFromInt(v.version),
      "vhash"    -> AttributeValue.S(v.vhash),
      "name"     -> AttributeValue.S(v.name),
      "location" -> AttributeValue.S(v.location)
    )
    if (v.description.isDefined) x.addOne("description", AttributeValue.S(v.description.get))
    Task
      .fromFuture(
        db.putItemFuture(
          PutItemInput(
            x,
            TableName = tableName.name
          )
        )
      )
      .map(_ => Right(()))
      .onErrorHandle(e => Left(e))
  }

  override def get(id: UUID): Task[Either[QueryError, Option[OrgView]]] =
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
            val name        = q.get("name").get.S.get
            val version     = q.get("version").get.N.get.toInt
            val vhash       = q.get("vhash").get.S.get
            val location    = q.get("location").get.S.get
            val description = q.get("description").map(p => p.S.get)
            OrgView(id, version, vhash, name, location, description)
          })
        )
      )
      .onErrorHandle(e => Left(SystemError(e.getMessage)))
}
