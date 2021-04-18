package me.mbcu.cqrs.query.service.otp

import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.Config.QueryCrypto
import me.mbcu.cqrs.shared.event.OtpToEmailRequested
import me.mbcu.cqrs.shared.http.HttpPayload
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import me.mbcu.cqrs.shared.util.UUID5
import org.scalatest.flatspec.AsyncFlatSpec

import java.util.UUID

class OtpServiceTest extends AsyncFlatSpec {
  implicit val ec                        = Config.ec
  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global // NEEDED !

  behavior of "OtpService"

  val config  = Config.load()
  val crypto  = QueryCrypto.fromConfig(config)
  val service = OtpService(config, crypto)

  val email   = "aaa@gmail.com"
  val id      = UUID5.v5(email)
  val iat     = System.currentTimeMillis() / 1000
  val exp     = iat + 10000
  val key     = UUID.randomUUID()
  val role    = Role.org
  val country = Country.ID
  val otp     = "12345"
  val event   = OtpToEmailRequested(id, email, 2, iat, key, otp, exp, role, country)

  it should "ok handling OtpEmailRequested event" in {
    service.handle(event).runToFuture.map(p => assert(p.isRight))
  }

  val inDto1 = InDto(email, key, otp, country, role)

  it should "ok authenticating with token returned" in {
    val payload = HttpPayload("", Map.empty, inDto1.asJson.noSpaces, None)
    service
      .fetch(payload)
      .value
      .runToFuture
      .map(p => assert(p.toOption.get.contains("\"token\"")))

  }

  val inDto2 = InDto("bbb@gmail.com", key, otp, country, role)

  it should "ko authenticating with bad credentials" in {
    val payload = HttpPayload("", Map.empty, inDto2.asJson.noSpaces, None)
    service
      .fetch(payload)
      .value
      .runToFuture
      .map(p => assert(p.isLeft))
  }
}
