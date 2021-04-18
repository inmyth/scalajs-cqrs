package me.mbcu.cqrs.shared.jwt

import me.mbcu.cqrs.shared.jwt.Alg.RS256
import me.mbcu.cqrs.shared.jwt.Typ.JWT
import enumeratum.{CirceEnum, Enum, EnumEntry}

final case class JwtHeader(alg: Alg, typ: Typ)

sealed trait Alg extends EnumEntry
object Alg extends Enum[Alg] with CirceEnum[Alg] {
  val values = findValues
  case object RS256 extends Alg
}

sealed trait Typ extends EnumEntry
case object Typ extends Enum[Typ] with CirceEnum[Typ] {
  val values = findValues
  case object JWT extends Typ
}

object JwtHeader {

  val typical: JwtHeader = new JwtHeader(RS256, JWT)

}
