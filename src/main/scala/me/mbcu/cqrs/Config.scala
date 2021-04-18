package me.mbcu.cqrs

import cats.data.Reader
import facade.amazonaws.services.dynamodb.DynamoDB
import facade.amazonaws.services.kms.KMS
import facade.amazonaws.services.sns.SNS
import me.mbcu.cqrs.Config.EnvMode
import me.mbcu.cqrs.command.crypto.Key
import me.mbcu.cqrs.command.publisher.Pub
import me.mbcu.cqrs.command.repository.EventStore
import me.mbcu.cqrs.query.crypto.QueryKey
import monix.execution.Scheduler

import scala.scalajs.js

final case class Config(
    envMode: EnvMode,
    snsTopic: String,
    cmkSignVerify: String,
    cmkEncryptDecrypt: String
)

object Config {

  sealed trait EnvMode
  case object Cloud extends EnvMode
  case object InMem extends EnvMode

  val ec: Scheduler = monix.execution.Scheduler.Implicits.global

  def load(): Config =
    js.Dynamic.global.process.env.RUN_MODE.asInstanceOf[js.UndefOr[String]].toOption match {
      case Some("cloud") =>
        Config(
          Cloud,
          js.Dynamic.global.process.env.SNS_TOPIC_ARN.asInstanceOf[js.UndefOr[String]].toOption.get,
//          js.Dynamic.global.process.env.SQS_PROJECTION_ARN.asInstanceOf[js.UndefOr[String]].toOption.get,
//          js.Dynamic.global.process.env.SQS_EVENTHANDLER_ARN.asInstanceOf[js.UndefOr[String]].toOption.get,
          js.Dynamic.global.process.env.CMK_SIGNVERIFY_ARN.asInstanceOf[js.UndefOr[String]].toOption.get,
          js.Dynamic.global.process.env.CMK_ENCRYPTDECRYPT_ARN.asInstanceOf[js.UndefOr[String]].toOption.get
        )

      case _ =>
        Config(
          InMem,
          "arn:sns",
          "arn:cmk_asym",
          "arn:cmk_sym"
        )
    }

  final case class WriteRepository(config: Config) {
    val db = new DynamoDB()

    val eventStore: EventStore = config.envMode match {
      case InMem => EventStore.inMem(Config.ec, config)
      case _     => EventStore.dynamo(Config.ec, config, db)
    }

  }

  object WriteRepository {
    val fromConfig: Reader[Config, WriteRepository] = Reader(WriteRepository(_))
  }

  final case class PublisherConfig(config: Config) {
    val sns = new SNS()

    val pub: Pub = config.envMode match {
      case InMem => Pub.fakeBroker(Config.ec, config)

      case _ => Pub.sns(Config.ec, config, sns)
    }

  }

  object PublisherConfig {
    val fromConfig: Reader[Config, PublisherConfig] = Reader(PublisherConfig(_))
  }

  final case class CommandCrypto(config: Config) {

    val kms = new KMS()

    val key: Key = config.envMode match {
      case Cloud => Key.kmsKey(config, kms)
      case InMem => Key.fakeKey(config)
    }

  }

  object CommandCrypto {
    val fromConfig: Reader[Config, CommandCrypto] = Reader(CommandCrypto(_))

  }

  final case class QueryCrypto(config: Config) {
    val kms = new KMS()

    val key: QueryKey = config.envMode match {
      case Cloud => QueryKey.kmsKey(config, kms)
      case InMem => QueryKey.fakeKey(config)
    }
  }

  object QueryCrypto {
    val fromConfig: Reader[Config, QueryCrypto] = Reader(QueryCrypto(_))

  }
}
