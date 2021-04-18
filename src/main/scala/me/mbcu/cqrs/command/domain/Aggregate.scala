package me.mbcu.cqrs.command.domain

import java.util.UUID

abstract case class Aggregate(aggregateId: UUID)

case class Projection[A](a: A, version: Int)
