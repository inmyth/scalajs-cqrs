package me.mbcu.cqrs.shared.dto

import org.scalatest.flatspec.AnyFlatSpec

class SecureDtoTest extends AnyFlatSpec {

//  behavior of "SecureDto"
//
//  val salt = "blabla"
//  case class Abc(i: Int, s: String, u: UUID, r: Role, salt: String = salt) extends Salted
//
//  it should "ok serializing deserializing salted object" in {
//    val uuid = UUID.randomUUID()
//    val role = Role.org
//    val i    = 1
//    val s    = "a"
//    val x    = Abc(i, s, uuid, role)
//    val y    = x.asJson.noSpaces
//    val z    = decode[Abc](y).toOption.get
//    assert(z.i === i)
//    assert(z.u === uuid)
//    assert(z.salt === salt)
//  }
//
//  it should "ok serializing deserializing secure dto" in {
//    val cipher = "abcdef"
//
//    val x = SecureDto(cipher)
//    val y = x.asJson.noSpaces
//    val z = decode[SecureDto](y).toOption.get
//    assert(z.cipherText === cipher)
//    assert(z.isSecure)
//
//  }
}
