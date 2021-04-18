package me.mbcu.cqrs.command.repository

import me.mbcu.cqrs.shared.event.{EsTable, Event}
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.DbOptimisticLockError
import me.mbcu.cqrs.command.repository.InMemImpl.db
import monix.eval.Task
import monix.execution.Scheduler

import java.util.UUID
import scala.collection.mutable

object InMemImpl {

  private val db: mutable.Map[String, Seq[(Int, Event)]] = mutable.Map.empty

}

final class InMemImpl(config: Config)(implicit scheduler: Scheduler) extends EventStore(scheduler, config) {

  override def loadLatest(aggregateId: UUID, esTable: EsTable): Task[Either[CommandError, Option[Event]]] =
    Task(
      Right(
        db.get(aggregateId.toString).map(p => p.last._2)
      )
    )

  override def loadAll(
      aggregateId: UUID,
      esTable: EsTable
  ): Task[Either[CommandError, Seq[Event]]] =
    Task(
      Right(
        db.getOrElse(aggregateId.toString, List.empty[(Int, Event)]).map(_._2)
      )
    )

  override def write(event: Event, esTable: EsTable): Task[Either[CommandError, Unit]] =
    Task {
      val stream = db.getOrElse(event.id.toString, Seq.empty[(Int, Event)])
      stream.lastOption match {
        case Some((version, _)) if version >= event.version => Left(DbOptimisticLockError(event.toString))
        case _ =>
          db.put(event.id.toString, stream :+ (event.version, event))
          Right()
      }
    }

  override def delete(event: Event, esTable: EsTable): Task[Either[CommandError, Unit]] =
    Task {
      val stream    = db.getOrElse(event.id.toString, Seq.empty[(Int, Event)])
      val newStream = stream.filterNot(_._1 == event.version)
      db.put(event.id.toString, newStream)
      Right()
    }
}
