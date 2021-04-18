package me.mbcu.cqrs.command.crypto

import me.mbcu.cqrs.shared.jwt.JwtContent
import facade.amazonaws.services.kms.{DecryptRequest, KMS, MessageType, SignRequest, SigningAlgorithmSpec, VerifyRequest}
import me.mbcu.cqrs.Config
import me.mbcu.cqrs.command.app.CommandError
import me.mbcu.cqrs.command.app.CommandError.SystemError
import monix.eval.Task

import java.util.Base64
import scala.scalajs.js
import scala.scalajs.js.typedarray.byteArray2Int8Array

abstract class Key(config: Config) {

  def sign(b64Encoded: String): Task[Either[Throwable, String]]

  def verify(jwtContent: JwtContent): Task[Either[Throwable, Unit]]

  def decrypt(base64Encoded: String): Task[Either[CommandError, String]]

}

object Key {

  def fakeKey(config: Config): Key = FakeKeyImpl(config)

  def kmsKey(config: Config, kms: KMS): Key = KmsImpl(config, kms)

}

final case class FakeKeyImpl(config: Config) extends Key(config) {

  val fakeJwt =
    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2tlcmFoYmlydS5jb20iLCJpYXQiOjE2MTQ5MDM3NjIsImV4cCI6MTgzNTc4MjQ4NCwic3ViIjoiNTY4YzE5NmYtMzE2ZS00NjFkLWE1NjktYTg3M2IwMTM3NzYyIiwiYWxpYXMiOiJtYmN1bWUiLCJyb2xlIjoib3JnIiwicGxhbiI6ImZyZWUifQ.n1jQis956exEgPTLUxwujyqDqrywdARk86NHqPAA5yI7Idz8pSXIImernbEtr7fFarLCu1OP8FQ6sKRsn9DNgonylC64MPkVNbA89sJWp8JUWA0VlsN/1lL8h7VbAIgQe47oSOgTYZEs6CknEzW/cl2N+wLXqgwt9SOVRAqW4hgFXXhD05kyfJ4yGDrJs0YJKJp8leRqnER5tK0eJBGoaOHr2tTS31wKQn8Hl5bGDrCBlBJtSHYOr31UGmyIqXfG/bVBxkU+rcOvGub9Ie9yYlVmlz6YtQAGLMdUGpa1JM8wEBfGTazedjd48b6tpTh7zg3hNngEyyehgFW83HHfeA=="

  override def sign(b64Encoded: String): Task[Either[Throwable, String]] =
    Task(
      Right(
        fakeJwt.split("[.]]")(2)
      )
    )

  override def verify(jwtContent: JwtContent): Task[Either[Throwable, Unit]] = Task.now(Right(()))

  override def decrypt(base64Encoded: String): Task[Either[CommandError, String]] = Task.now(Right("xxx"))

}

final case class KmsImpl(config: Config, kms: KMS) extends Key(config) {

  override def sign(b64Encoded: String): Task[Either[Throwable, String]] =
    Task
      .fromFuture(
        kms
          .sign(
            SignRequest(
              KeyId = config.cmkSignVerify,
              Message = b64Encoded,
              SigningAlgorithm = SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256,
              MessageType = MessageType.RAW
            )
          )
          .promise()
          .toFuture
      )
      .map(p =>
        p.Signature.toOption match {
          case Some(value) => Right(Base64.getEncoder.encodeToString(value.asInstanceOf[js.Array[scala.Byte]].toArray))
          case None        => Left(new Throwable("KMS Sign returns undefined"))
        }
      )
      .onErrorHandle(e => Left(e))

  override def verify(jwtContent: JwtContent): Task[Either[Throwable, Unit]] =
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
      .map(p => if (p.SignatureValid.isDefined) Right(()) else Left(new Throwable("verification error")))
      .onErrorHandle(_ => Left(new Throwable("verification error")))

  override def decrypt(base64Encoded: String): Task[Either[CommandError, String]] =
    Task
      .fromFuture(
        kms.decryptFuture(
          DecryptRequest(
            CiphertextBlob = byteArray2Int8Array(Base64.getDecoder.decode(base64Encoded)),
            KeyId = config.cmkEncryptDecrypt
          )
        )
      )
      .map(p =>
        p.Plaintext.toOption match {
          case Some(value) => Right(value.toString)
          case None        => Left(SystemError(s"No decryption result for $base64Encoded"))
        }
      )
      .onErrorRecover { case e => Left(SystemError(e.getMessage)) }

}
