package me.mbcu.cqrs.command.app

import me.mbcu.cqrs.command.app.CommandError.ForbiddenAccess
import me.mbcu.cqrs.command.domain.org.CreateOrg
import me.mbcu.cqrs.shared.http.HttpPayload
import me.mbcu.cqrs.shared.jwt.{Country, Role}
import me.mbcu.cqrs.shared.util.UUID5
import me.mbcu.cqrs.{Config, TestUtil}
import org.scalatest.flatspec.AsyncFlatSpec

class HttpControllerTest extends AsyncFlatSpec with TestUtil {
  implicit val ec                        = Config.ec
  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global // NEEDED !
  /*
  - open accessing secure (no token)
  - token role not matching command role
  - secure accessing secure
  - open accessing open
   */

  behavior of "HttpController (with CreateOrg command)"

  val mucoId = UUID5.v5("muco")

  val rawCreate =
    """{
    |"name": "muco",
    |"location": "Jakarta"
    |}
    |""".stripMargin

  val path = "https://abc.com/org/abcdefg"

  val payload = HttpPayload.build(
    path,
    Map.empty,
    rawCreate,
    Some(makeFakeToken(mucoId, Role.org, Country.ID))
  )

  val httpController = HttpController.fromConfig(Config.load())

  it should "ok getting the right route, rejecting bad path" in {
    val x = httpController.route(payload)
    val badPayload = HttpPayload.build(
      "https://abc.com/aaa",
      Map.empty,
      rawCreate,
      Some(makeFakeToken(mucoId, Role.org, Country.ID))
    )
    val y = httpController.route(badPayload)
    assert(x.isRight)
    assert(y.isLeft)

  }

  it should "ok verifying well formatted token" in {
    val c = httpController.route(payload).toOption.get
    val d = httpController.verifyToken(c.access, payload.token)
    d.runToFuture.map(p => {
      println(p)
      assert(p.isRight)
    })
  }

  it should "ko verifying a non-token" in {
    val badPayload = HttpPayload.build(
      path,
      Map.empty,
      rawCreate,
      Some("aaa.bbb.ccc")
    )
    val c = httpController.route(badPayload).toOption.get
    val d = httpController.verifyToken(c.access, badPayload.token)
    d.runToFuture.map(p => assert(p === Left(ForbiddenAccess)))
  }

  it should "ok passing / rejecting unexpired/expired token" in {
    val x = System.currentTimeMillis() / 1000 + 1000
    val y = x - 10000;
    assert(httpController.checkTokenExpiration(x).isRight)
    assert(httpController.checkTokenExpiration(y).isLeft)
  }

  it should "ok passing/rejecting jwt role/ allowed command role" in {
    val ja = Role.org
    val ca = Some(Role.org)
    val jb = Role.worker
    assert(httpController.checkRoleAllowed(ja, ca))
    assert(!httpController.checkRoleAllowed(jb, ca))
  }

  it should "ok building command with good payload" in {
    httpController
      .buildCommand(payload)
      .value
      .runToFuture
      .map(p => assert(p.toOption.get.isInstanceOf[CreateOrg]))
  }

}
