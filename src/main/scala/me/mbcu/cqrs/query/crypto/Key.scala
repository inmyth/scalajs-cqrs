package me.mbcu.cqrs.query.crypto

import cats.Monad
import cats.data.EitherT
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.query.app.QueryError
import me.mbcu.cqrs.query.app.QueryError.{BadJwt, SystemError}
import me.mbcu.cqrs.shared.jwt.{JwtContent, JwtHeader, JwtPayload, Token}
import facade.amazonaws.services.kms.{
  EncryptRequest,
  KMS,
  MessageType,
  SignRequest,
  SigningAlgorithmSpec,
  VerifyRequest
}
import monix.eval.Task

import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

abstract class QueryKey(config: Config) {

  def verify(jwtContent: JwtContent): Task[Either[QueryError, Unit]]

  def encrypt(plainText: String): Task[Either[QueryError, String]]

  def sign(jwtPayload: JwtPayload): Task[Either[QueryError, String]]

}

object QueryKey {

  def fakeKey(config: Config): QueryKey = FakeKeyImpl(config)

  def kmsKey(config: Config, kms: KMS): QueryKey = KmsImpl(config, kms)

}

private[query] final case class FakeKeyImpl(config: Config) extends QueryKey(config) {

  val fakeJwt =
    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2tlcmFoYmlydS5jb20iLCJpYXQiOjE2MTQ5MDM3NjIsImV4cCI6MTgzNTc4MjQ4NCwic3ViIjoiNTY4YzE5NmYtMzE2ZS00NjFkLWE1NjktYTg3M2IwMTM3NzYyIiwiYWxpYXMiOiJtYmN1bWUiLCJyb2xlIjoib3JnIiwicGxhbiI6ImZyZWUifQ.n1jQis956exEgPTLUxwujyqDqrywdARk86NHqPAA5yI7Idz8pSXIImernbEtr7fFarLCu1OP8FQ6sKRsn9DNgonylC64MPkVNbA89sJWp8JUWA0VlsN/1lL8h7VbAIgQe47oSOgTYZEs6CknEzW/cl2N+wLXqgwt9SOVRAqW4hgFXXhD05kyfJ4yGDrJs0YJKJp8leRqnER5tK0eJBGoaOHr2tTS31wKQn8Hl5bGDrCBlBJtSHYOr31UGmyIqXfG/bVBxkU+rcOvGub9Ie9yYlVmlz6YtQAGLMdUGpa1JM8wEBfGTazedjd48b6tpTh7zg3hNngEyyehgFW83HHfeA=="

  override def verify(jwtContent: JwtContent): Task[Either[QueryError, Unit]] = Task.now(Right(()))

  override def encrypt(plainText: String): Task[Either[QueryError, String]] = Task.now(Right("aaa"))

  override def sign(jwtPayload: JwtPayload): Task[Either[QueryError, String]] = Task.now(Right("bbb"))
}

private[query] final case class KmsImpl(config: Config, kms: KMS) extends QueryKey(config) with Token[Task] {

  override def verify(jwtContent: JwtContent): Task[Either[QueryError, Unit]] =
    Task
      .fromFuture {
        kms
          .verifyFuture(
            VerifyRequest(
              config.cmkSignVerify,
              jwtContent.msg,
              jwtContent.sig,
              SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256,
              {},
              MessageType.RAW
            )
          )
      }
      .map(p => if (p.SignatureValid.isDefined) Right(()) else Left(BadJwt))
      .onErrorHandle(e => Left(SystemError(e.getMessage)))

  override def encrypt(plainText: String): Task[Either[QueryError, String]] =
    Task
      .fromFuture {
        kms.encryptFuture(EncryptRequest(KeyId = config.cmkEncryptDecrypt, Plaintext = plainText))
      }
      .map(p => Right(Base64.getEncoder.encodeToString(p.CiphertextBlob.asInstanceOf[js.Array[scala.Byte]].toArray)))
      .onErrorRecover { case e => Left(SystemError(e.getMessage)) }

  override val F: Monad[Task] = Monad[Task]

  override def sign(content: String): Task[String] =
    Task
      .fromFuture(
        kms
          .sign(
            SignRequest(
              KeyId = config.cmkSignVerify,
              Message = content,
              SigningAlgorithm = SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256,
              MessageType = MessageType.RAW
            )
          )
          .promise()
          .toFuture
      )
      .map(p => Base64.getEncoder.encodeToString(p.Signature.toOption.get.asInstanceOf[js.Array[scala.Byte]].toArray))

  override def sign(jwtPayload: JwtPayload): Task[Either[QueryError, String]] =
    create(jwtPayload)
      .map(p => Right(p))
      .onErrorHandle { _ => Left(SystemError("Cannot sign token with cmk sign")) }

}
