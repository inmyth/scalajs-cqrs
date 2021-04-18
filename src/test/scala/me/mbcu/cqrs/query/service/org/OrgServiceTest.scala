package me.mbcu.cqrs.query.service.org

import me.mbcu.cqrs.Config.QueryCrypto
import me.mbcu.cqrs.shared.event.OrgCreated
import me.mbcu.cqrs.shared.http.HttpPayload
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import me.mbcu.cqrs.shared.util.UUID5
import me.mbcu.cqrs.{Config, TestUtil}
import org.scalatest.flatspec.AsyncFlatSpec

import java.util.UUID

class OrgServiceTest extends AsyncFlatSpec with TestUtil {
  implicit val ec                        = Config.ec
  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global // NEEDED !

  behavior of "OtpService"

  val config  = Config.load()
  val crypto  = QueryCrypto.fromConfig(config)
  val service = OrgService(config)

  val email       = "aaa@gmail.com"
  val id          = UUID5.v5(email)
  val name        = "muco"
  val location    = "jakarta"
  val description = Some("aaa bb cccc")
  val created     = OrgCreated(id, name, location, description)

  behavior of "OrgService"

  it should "ok handling OrgCreated" in {
    service.handle(created).runToFuture.map(p => assert(p.isRight))
  }

  it should "ok fetching data" in {
    val x = makeJwtContent(id, Role.org, Country.ID, "")
    service
      .fetch(HttpPayload("", Map.empty, "", Some(x)))
      .value
      .runToFuture
      .map(p => assert(p.toOption.get.contains("\"description\":\"aaa bb cccc\"")))
  }

  it should "ok fetching data with non-existent id" in {
    val x = makeJwtContent(UUID.randomUUID(), Role.org, Country.ID, "")
    service
      .fetch(HttpPayload("", Map.empty, "", Some(x)))
      .value
      .runToFuture
      .map(p => assert(p.toOption.get.contains("\"data\":null")))
  }

}
