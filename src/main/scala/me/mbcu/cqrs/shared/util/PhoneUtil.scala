package me.mbcu.cqrs.shared.util

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("libphonenumber-js", JSImport.Namespace)
object PhoneUtil extends js.Object {

  def isValidPhoneNumber(x: String, countryCode: String): Boolean = js.native

  def parsePhoneNumberWithError(x: String, countryCode: String): js.UndefOr[PhoneNumber] = js.native

}

@js.native
@JSImport("libphonenumber-js", "PhoneNumber")
class PhoneNumber(
    var number: String,
    val countryCallingCode: String,
    val nationalNumber: String,
    val metadata: js.Object
) extends js.Object
