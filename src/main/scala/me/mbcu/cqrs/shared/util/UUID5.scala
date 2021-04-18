package me.mbcu.cqrs.shared.util

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("uuid", JSImport.Namespace)
object NodeUUID extends js.Object {

  val NIL: String = js.native

  def v5(x: String, nameSpace: String): String = js.native

}

object UUID5 {
  def v5(x: String): UUID = UUID.fromString(NodeUUID.v5(x, NodeUUID.NIL))
}
