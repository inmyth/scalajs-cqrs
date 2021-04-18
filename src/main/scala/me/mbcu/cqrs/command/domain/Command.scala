package me.mbcu.cqrs.command.domain

import me.mbcu.cqrs.shared.jwt.Role
import me.mbcu.cqrs.Config.CommandCrypto
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.shared.http.HttpPayload
import monix.eval.Task

import java.util.UUID

abstract class Command(val aggregateId: UUID) {

  def allowPermission: Option[Role]

  def getCommandHandler: CommandHandler[_]

}

trait CommandBuilder {

  def build(payload: HttpPayload, crypto: CommandCrypto): Task[Either[CommandError, Command]]

}
