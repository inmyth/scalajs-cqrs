package me.mbcu.cqrs.shared.event

import enumeratum._

sealed trait EventName extends EnumEntry

object EventName extends Enum[EventName] with CirceEnum[EventName] {

  val values: IndexedSeq[EventName] = findValues

  // org
  case object OrgCreated extends EventName
  case object OrgUpdated extends EventName

  // otp
  case object OtpToEmailRequested extends EventName
  case object OtpToSmsRequested   extends EventName

  // authenticate
  case object UserSignedIn extends EventName
}
