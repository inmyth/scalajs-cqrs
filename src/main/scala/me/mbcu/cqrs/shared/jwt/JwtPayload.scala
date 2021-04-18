package me.mbcu.cqrs.shared.jwt

import enumeratum._

import java.util.UUID

final case class JwtPayload(
    iss: String = "https://www.kerahbiru.com",
    iat: Long,
    exp: Long,
    sub: UUID,
    alias: String,
    role: Role,
    country: Country
)

sealed trait Role extends EnumEntry
object Role extends Enum[Role] with CirceEnum[Role] {
  val values = findValues
  case object worker extends Role
  case object org    extends Role
}

sealed trait Country extends EnumEntry
object Country extends Enum[Country] with CirceEnum[Country] {

  val values = findValues
  case object ID extends Country
  case object PH extends Country
  case object VN extends Country
  case object SG extends Country
  case object MY extends Country
  case object TH extends Country
  case object KH extends Country
  case object LA extends Country
  case object MM extends Country
  case object BN extends Country
  case object JP extends Country
  case object TW extends Country
  case object KR extends Country
  case object HK extends Country
}
