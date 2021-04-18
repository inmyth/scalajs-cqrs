package me.mbcu.cqrs.shared.event

import java.util.UUID

class Event(
    val id: UUID,
    val version: Int,
    val vhash: String,
    val iat: Long,
    val user: UUID,
    val aggregate: AggregateName,
    val name: EventName,
    val data: String
)

object Event {

  def vhash(version: Int): String = UUID.randomUUID().toString.split("[-]")(0)

//  implicit val dec: Decoder[Event] = deriveDecoder
//  implicit val enc: Encoder[Event] = deriveEncoder

//  implicit val decodeUuidKey: KeyDecoder[UUID] =
//    KeyDecoder.instance(s => Try(UUID.fromString(s)).toOption)
//
//  implicit val encodeUuidKey: KeyEncoder[UUID] =
//    KeyEncoder.instance(_.toString)
}
