package me.mbcu.cqrs.command.repository

import me.mbcu.cqrs.shared.event.{AggregateName, EsTable, Event, EventName}
import facade.amazonaws.services.dynamodb.{AttributeValue, DeleteItemInput, DynamoDB, PutItemInput, QueryInput, QueryOutput}
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.DatabaseError
import monix.eval.Task
import monix.execution.Scheduler

import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.scalajs.js
import scala.scalajs.js.Dictionary

object DynamoImpl {}

class DynamoImpl(config: Config, db: DynamoDB)(implicit scheduler: Scheduler) extends EventStore(scheduler, config) {
  // |  id|  version|iat|user|agg|name|data

  override def loadAll(
      aggregateId: UUID,
      esTable: EsTable
  ): Task[Either[CommandError, Seq[Event]]] =
    accumulate(aggregateId, esTable, ListBuffer.empty, createLoadTask(aggregateId, esTable, None))
      .map(p => { Right(p.toSeq) })
      .onErrorHandle(e => Left(DatabaseError(e.getMessage)))

  def createLoadTask(
      aggregateId: UUID,
      esTable: EsTable,
      startKey: Option[facade.amazonaws.services.dynamodb.Key]
  ): Task[QueryOutput] =
    Task
      .fromFuture(
        db.queryFuture(
          QueryInput(
            TableName = esTable.name,
            KeyConditionExpression = "id = :v1",
            ExpressionAttributeValues = Dictionary(
              ":v1" -> AttributeValue.S(aggregateId.toString)
            ),
            ExclusiveStartKey = if (startKey.isEmpty) js.undefined else startKey.get
          )
        )
      )

  def accumulate(
      id: UUID,
      esTable: EsTable,
      accum: ListBuffer[Event],
      source: Task[QueryOutput]
  ): Task[ListBuffer[Event]] =
    source.flatMap(p => {
      val lastKey = p.LastEvaluatedKey.toOption
      val events = p.Items.get.map(q => {
        val version = q.get("version").get.N.get.toInt
        val vhash   = q.get("vhash").get.S.get
        val iat     = q.get("iat").get.N.get.toLong
        val user    = UUID.fromString(q.get("user").get.S.get)
        val agg     = AggregateName.withName(q.get("agg").get.S.get)
        val name    = EventName.withName(q.get("name").get.S.get)
        val data    = q.get("data").get.S.get
        new Event(id, version, vhash, iat, user, agg, name, data)
      })
      accum ++= events
      if (lastKey.isEmpty) {
        Task.now(accum)
      } else {
        accumulate(id, esTable, accum, createLoadTask(id, esTable, lastKey))
      }
    })

  override def loadLatest(aggregateId: UUID, esTable: EsTable): Task[Either[CommandError, Option[Event]]] =
    Task
      .fromFuture(
        db.queryFuture(
          QueryInput(
            TableName = esTable.name,
            KeyConditionExpression = "id = :v1",
            ExpressionAttributeValues = Dictionary(
              ":v1" -> AttributeValue.S(aggregateId.toString)
            ),
            Limit = 1,
            ScanIndexForward = false
          )
        )
      )
      .map(p => {
        val events = p.Items.get.map(q => {
          val version = q.get("version").get.N.get.toInt
          val vhash   = q.get("vhash").get.S.get
          val iat     = q.get("iat").get.N.get.toLong
          val user    = UUID.fromString(q.get("user").get.S.get)
          val agg     = AggregateName.withName(q.get("agg").get.S.get)
          val name    = EventName.withName(q.get("name").get.S.get)
          val data    = q.get("data").get.S.get
          new Event(aggregateId, version, vhash, iat, user, agg, name, data)
        })
        Right(if (events.isEmpty) None else Some(events(0)))
      })
      .onErrorHandle(e => Left(DatabaseError(e.getMessage)))

  override def write(event: Event, esTable: EsTable): Task[Either[CommandError, Unit]] =
    Task
      .fromFuture(
        db.putItemFuture(
          PutItemInput(
            Item = Dictionary(
              "id"      -> AttributeValue.S(event.id.toString),
              "version" -> AttributeValue.NFromInt(event.version),
              "vhash"   -> AttributeValue.S(event.vhash),
              "iat"     -> AttributeValue.NFromLong(event.iat),
              "user"    -> AttributeValue.S(event.user.toString),
              "agg"     -> AttributeValue.S(event.aggregate.entryName),
              "name"    -> AttributeValue.S(event.name.entryName),
              "data"    -> AttributeValue.S(event.data)
            ),
            TableName = esTable.name
          )
        )
      )
      .map(_ => Right(()))
      .onErrorHandle(e => Left(DatabaseError(e.getMessage)))

  override def delete(event: Event, esTable: EsTable): Task[Either[CommandError, Unit]] =
    Task
      .fromFuture(
        db.deleteItemFuture(
          DeleteItemInput(
            Key = Dictionary(
              "id"      -> AttributeValue.S(event.id.toString),
              "version" -> AttributeValue.NFromInt(event.version)
            ),
            TableName = esTable.name
          )
        )
      )
      .map(_ => Right(()))
      .onErrorHandle {
        case e if e.getMessage.contains("ResourceNotFoundException") => Right(())
        case e                                                       => Left(DatabaseError(e.getMessage))
      }

}
