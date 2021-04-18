package me.mbcu.cqrs.shared.format

import io.circe.syntax.EncoderOps
import me.mbcu.cqrs.shared.http.Response
import org.scalatest.flatspec.AnyFlatSpec
import io.circe.generic.auto._

class ResponseTest extends AnyFlatSpec {

  behavior of "Response"

  it should "ok encoding with any A" in {
    val data = List("hello")

    val x = Response.ok[List[String]](data).asJson.noSpaces
    assert(x.contains("hello"))
  }

  it should "ok encoding error" in {
    val x = Response.error("something bad").asJson.noSpaces
    assert(x.contains("bad"))
  }

  it should "ok encoding OneMesage" in {
    val x = Response.just("success").asJson.noSpaces
    assert(x.contains("success"))
  }

  it should "ok encoding error with OneMessage type" in {
    val x = Response.error("something bad").asJson.noSpaces
    assert(x.contains("bad"))
  }

  it should "ok encoding option" in {
    val data = None
    val x    = Response.ok[Option[String]](data).asJson.noSpaces
    assert(x.contains("\"data\":null}"))
  }

}
