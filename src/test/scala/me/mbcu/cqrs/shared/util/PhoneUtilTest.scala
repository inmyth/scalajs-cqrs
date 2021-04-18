package me.mbcu.cqrs.shared.util

import me.mbcu.cqrs.shared.jwt.Country
import org.scalatest.flatspec.AnyFlatSpec

import scala.scalajs.js
import scala.util.Try

class PhoneUtilTest extends AnyFlatSpec {

  behavior of "PhoneUtil#isValidPhoneNumber"

  it should "ok combination of common numbers" in {
    val x = List("+6221 3971 0909", "021 3971 0909", "21-3971-0909")
      .map(p => PhoneUtil.isValidPhoneNumber(p, Country.ID.entryName))
      .filter(_ == false)
    assert(x.isEmpty)
  }

  it should "ko combination of bad numbers" in {
    val x = List("+43fgs3333", "021 3971 0909 6445 5654354", "1335654667567567")
      .map(p => PhoneUtil.isValidPhoneNumber(p, Country.ID.entryName))
      .filter(_ == false)
    assert(x.size == 3)
  }

  behavior of "PhoneUtil#parsePhoneNumberWithError"

  it should "return the same E.164 with combination of similar numbers" in {
    val expected = "+622139710909"
    val x = List("+6221 3971 0909", "021 3971 0909", "21-3971-0909")
      .map(p => PhoneUtil.parsePhoneNumberWithError(p, Country.ID.entryName))
      .filter(_ != js.undefined)
      .map(_.asInstanceOf[PhoneNumber])
      .map(_.number)
      .filter(_ == expected)
    assert(x.size == 3)
  }

  it should "fail with too long numbers" in {
    val x = List("2434534534534535435345")
      .map(p => Try(PhoneUtil.parsePhoneNumberWithError(p, Country.ID.entryName)))
      .filter(_.isFailure)
    assert(x.size == 1)
  }

  it should "fail with not right numbers" in {
    val x = List("+++43532k34242", "+324+234+234")
      .map(p => Try(PhoneUtil.parsePhoneNumberWithError(p, Country.ID.entryName)))
      .filter(_.isFailure)
    assert(x.size == 2)
  }

}
