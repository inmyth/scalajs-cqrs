package me.mbcu.cqrs

import cats.Monad
import io.circe.generic.auto._
import io.circe.parser.decode
import me.mbcu.cqrs.shared.jwt.{Country, JwtContent, JwtHeader, JwtPayload, Role, Token}

import java.util.UUID

trait TestUtil {

  val rawHeader =
    """
      |{
      |"alg": "RS256",
      |"typ": "JWT"
      |}
      |""".stripMargin

  val jwtHeader = decode[JwtHeader](rawHeader).toOption.get

  private def createRawPayload(sub: UUID, role: Role, country: Country) = s"""
    |{
    |  "iss": "https://www.kerahbiru.com",
    |  "iat": "1614903762",
    |  "exp": "1835782484",
    |  "sub": "${sub.toString}",
    |  "alias": "mbcu",
    |  "role": "${role.entryName}",
    |  "country": "${country.entryName}"
    |}
    |""".stripMargin

  val sign = "xxx"

  case class TokenImpl() extends Token[Option] {
    override val F: Monad[Option] = Monad[Option]

    override def sign(content: String): Option[String] = Some(TestUtil.this.sign)
  }

  val tokenImpl: TokenImpl = TokenImpl()

  def makeFakeToken(sub: UUID, role: Role, country: Country): String = {
    val x = JwtPayload(
      iat = System.currentTimeMillis() / 1000,
      exp = System.currentTimeMillis() / 1000 + 10000,
      sub = sub,
      alias = sub.toString,
      country = country,
      role = role
    )
    tokenImpl.create(x).get
  }

  def makeJwtContent(sub: UUID, role: Role, country: Country, msg: String): JwtContent =
    JwtContent(jwtHeader, decode[JwtPayload](createRawPayload(sub, role, country)).toOption.get, msg, null)

}
