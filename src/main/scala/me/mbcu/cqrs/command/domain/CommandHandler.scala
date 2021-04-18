package me.mbcu.cqrs.command.domain

import cats.data.EitherT
import me.mbcu.cqrs.shared.event.{EsTable, Event}
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.repository.EventStore
import monix.eval.Task

import java.util.UUID

trait CommandHandler[A] {

  def empty: A

  def esTable: EsTable

  def shouldLoadAll: Boolean = true

  def replay(stream: Seq[Event]): Projection[A] =
    stream.foldLeft(Projection(empty, -1)) { (projection, event) =>
      Projection(apply(projection.a, event), event.version)
    }

//    stream.foldLeft(Right(Projection(empty, -1)).withLeft[CommandError])((a, b) =>
//      for {
//        p <- a
//        q <- Right(apply(p.a, b))
//        r <- Right(Projection(q, b.version))
//      } yield r
//    )

  def apply(a: A, event: Event): A

  def decide(projection: Projection[A], command: Command): Either[CommandError, Event]

  def load(es: EventStore, aggregateId: UUID): Task[Either[CommandError, Seq[Event]]] =
    if (shouldLoadAll) es.loadAll(aggregateId, esTable)
    else es.loadLatest(aggregateId, esTable).map(p => p.map(q => q.toList))

  def write(es: EventStore, event: Event): Task[Either[CommandError, Unit]] = es.write(event, esTable)

  def delete(es: EventStore, event: Event): Task[Either[CommandError, Unit]] = es.delete(event, esTable)

  def handleCommand(es: EventStore, command: Command): EitherT[Task, CommandError, Event] =
    for {
      a <- EitherT(load(es, command.aggregateId))
      b <- EitherT(Task(Right(replay(a)).withLeft[CommandError]))
      c <- EitherT(Task(decide(b, command)))
      _ <- EitherT(es.write(c, esTable))
    } yield c

}
