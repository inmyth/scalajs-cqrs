package me.mbcu.cqrs.query.app

import me.mbcu.cqrs.Config
import me.mbcu.cqrs.Config.QueryCrypto
import me.mbcu.cqrs.query.app.QueryError.ForbiddenAccess
import me.mbcu.cqrs.shared.http.{HttpPayload, secure}
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import org.scalatest.flatspec.AsyncFlatSpec

import java.util.UUID

class QueryAppTest extends AsyncFlatSpec {
  implicit val ec                        = Config.ec
  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global // NEEDED !

  behavior of "QueryApp (with OtpService)"

  val email   = "aaa@gmail.com"
  val key     = UUID.randomUUID()
  val otp     = "1267"
  val country = Country.ID
  val role    = Role.org

  val request =
    s"""
      |{
      |"primary": "$email",
      |"key": "${key.toString}",
      |"otp": "$otp",
      |"country": "${country.entryName}",
      |"role": "${role.entryName}"
      |}
      |""".stripMargin

  val payload = HttpPayload("www.abc.com/authenticate", Map.empty, request, None)

  val config = Config.load
  val app    = QueryApp(QueryCrypto.fromConfig(config))

  it should "ok matching route" in {
    val x = app.route(payload)
    val y = app.route(HttpPayload("www.abc.com/aaa", Map.empty, request, None))
    assert(x.isRight)
    assert(y.isLeft)
  }

  it should "ko passing passing no-token to a secure query" in {
    val d = app.verifyToken(secure, payload.token)
    d.runToFuture.map(p => assert(p === Left(ForbiddenAccess)))
  }

  it should "ok passing / rejecting unexpired/expired token" in {
    val x = System.currentTimeMillis() / 1000 + 1000
    val y = x - 10000;
    assert(app.checkTokenExpiration(x).isRight)
    assert(app.checkTokenExpiration(y).isLeft)
  }

  it should "ok passing/rejecting jwt role/ allowed command role" in {
    val ja = Role.org
    val ca = Some(Role.org)
    val jb = Role.worker
    assert(app.checkRoleAllowed(ja, ca))
    assert(!app.checkRoleAllowed(jb, ca))
  }

  it should "ok executing a query with negative result " in {
    app
      .process(payload)
      .value
      .runToFuture
      .map(p => assert(p.isLeft))

  }
}
