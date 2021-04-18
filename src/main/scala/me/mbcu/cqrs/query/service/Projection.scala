package me.mbcu.cqrs.query.service

import me.mbcu.cqrs.shared.event.Event
import monix.eval.Task

trait Projection {

  def handle(event: Event): Task[Either[Throwable, Unit]]

}
