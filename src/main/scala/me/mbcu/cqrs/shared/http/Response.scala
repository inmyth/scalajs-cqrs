package me.mbcu.cqrs.shared.http

final case class Response[A](isError: Boolean, isEncrypted: Boolean, error: Option[String], data: A)

object Response {

  def ok[A](data: A): Response[A] = Response(isError = false, isEncrypted = false, None, data)

  def okEncrypted[A](data: A): Response[A] = Response(isError = false, isEncrypted = true, None, data)

  def error(error: String): Response[Empty] =
    Response[Empty](isError = true, isEncrypted = false, Some(error), Empty())

  def just(message: String): Response[OneMessage] = Response.ok[OneMessage](new OneMessage(message))

  abstract class Salted {
    val salt: String
  }

  abstract class Vhashed {
    val vhash: String
  }

}

final case class Empty()

final case class OneMessage(message: String)
