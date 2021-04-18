package me.mbcu.cqrs.command.repository

import me.mbcu.cqrs.shared.event.{EsTable, Event}
import facade.amazonaws.services.dynamodb.DynamoDB
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.command.app.CommandError
import monix.eval.Task
import monix.execution.Scheduler

import java.util.UUID

abstract class EventStore(val scheduler: Scheduler, val config: Config) {

  def loadLatest(aggregateId: UUID, esTable: EsTable): Task[Either[CommandError, Option[Event]]]

  def loadAll(aggregateId: UUID, esTable: EsTable): Task[Either[CommandError, Seq[Event]]]

  def write(event: Event, esTable: EsTable): Task[Either[CommandError, Unit]]

  def delete(event: Event, esTable: EsTable): Task[Either[CommandError, Unit]]
}

object EventStore {

  def inMem(scheduler: Scheduler, config: Config): EventStore = new InMemImpl(config)(scheduler)

  def dynamo(scheduler: Scheduler, config: Config, db: DynamoDB): EventStore = new DynamoImpl(config, db)(scheduler)

}
