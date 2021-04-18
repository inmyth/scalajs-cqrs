package me.mbcu.cqrs.shared.event

import enumeratum.{CirceEnum, Enum, EnumEntry}

trait Meta {

  def aggregateName: AggregateName

  def eventName: EventName
}

sealed trait AggregateName extends EnumEntry

object AggregateName extends Enum[AggregateName] with CirceEnum[AggregateName] {

  val values: IndexedSeq[AggregateName] = findValues

  case object org  extends AggregateName
  case object otp  extends AggregateName
  case object user extends AggregateName

}

case class EsTable(aggregateName: AggregateName) {

  def name: String = s"es_${aggregateName.entryName}"
}

case class QueryTable(aggregateName: AggregateName) {
  def name: String = s"query_${aggregateName.entryName}"
}
