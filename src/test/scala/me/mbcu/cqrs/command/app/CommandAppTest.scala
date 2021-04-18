package me.mbcu.cqrs.command.app

import me.mbcu.cqrs.Config
import org.scalatest.flatspec.AsyncFlatSpec

class CommandAppTest extends AsyncFlatSpec {

  implicit val ec                        = Config.ec
  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global // NEEDED !

//  def fixture =
//    new {
//      lazy val id = UUID.randomUUID();
//
//      val createJson =
//        """{
//          |"name": "muco",
//          |"location": "Jakarta"
//          |}
//          |""".stripMargin
//      val create = CreateOrg.fromDto(createJson, id)
//
//      val updateJson =
//        """
//          |{
//          |"name":"muco",
//          |"location":"Tangerang"
//          |}
//          |""".stripMargin
//      val update = UpdateOrg.fromDto(updateJson, id)
//    }
//
//  behavior of "org Service"
//
//  it should "ok when create is run against empty stream" in {
//    val f   = fixture
//    val app = CommandApp.fromConfig.run(Config.load())
//    app.process().value.runToFuture map (p => {
//      p.map(q => {
//        assert(q.id === f.id)
//        assert(q.name === EventName.OrgCreated)
//      })
//      assert(p.isRight)
//    })
//  }
//
//  it should "ko when update is run on empty stream," in {
//    val f       = fixture
//    val service = CommandApp.fromConfig.run(Config.load())
//    service.process(f.update.toOption.get).value.runToFuture map (p => {
//      assert(p.isLeft)
//    })
//  }
//
//  it should "ko when create follows a create," in {
//    val f       = fixture
//    val service = CommandApp.fromConfig.run(Config.load())
//    val x = for {
//      _ <- service.process(f.create.toOption.get)
//      a <- service.process(f.create.toOption.get)
//    } yield a
//    x.value.runToFuture map (p => {
//      assert(p.isLeft)
//    })
//
//  }
//
//  it should "ok when update follows a create," in {
//    val service = CommandApp.fromConfig.run(Config.load())
//    val f       = fixture
//    val x = for {
//      _ <- service.process(f.create.toOption.get)
//      a <- service.process(f.update.toOption.get)
//    } yield a
//    x.value.runToFuture map (p => {
//      println(p)
//      p.map(q => {
//        assert(q.id === f.id)
//        val json = decode[OrgUpdated.Data](q.data)
//        json.map(r => r.location === f.update.toOption.get.location.value)
//        assert(q.name === EventName.OrgUpdated)
//      })
//      assert(p.isRight)
//    })
//
//  }

}
