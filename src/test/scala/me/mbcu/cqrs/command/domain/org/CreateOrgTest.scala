package me.mbcu.cqrs.command.domain.org

import org.scalatest.flatspec.AnyFlatSpec

import java.util.UUID

class CreateOrgTest extends AnyFlatSpec {

  val uuid = UUID.randomUUID()

//  behavior of "CreateOrg"
//  it should "succeed building from dto with missing description" in {
//    val rawJson: String = """
//      {
//        "name": "bar",
//        "location": "123"
//      }
//      """
//    val x               = CreateOrg.build(rawJson, uuid)
//    assert(x.isRight)
//  }
}
