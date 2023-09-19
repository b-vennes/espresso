import smithy4s.hello._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import espresso._

object HelloWorldImpl extends HelloWorldService[IO] {
  def hello(name: String, town: Option[String]): IO[Greeting] = IO.pure {
    town match {
      case None => Greeting(s"Hello $name!")
      case Some(t) => Greeting(s"Hello $name from $t!")
    }
  }
}

object Routes {
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldImpl).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
}

object Main extends IOApp.Simple {

//  val run = Routes.all
//    .flatMap { routes =>
//      EmberServerBuilder
//        .default[IO]
//        .withPort(port"9000")
//        .withHost(host"localhost")
//        .withHttpApp(routes.orNotFound)
//        .build
//    }
//    .use(_ => IO.never)

  def person(name: String) = EntityType.withJsonEncoding[Person, PersonEvent](
    "Person",
    Person(name, None),
    (p, e) => Person(e.name.getOrElse(""), e.town)
  )

  case class PersonEntity(name: String) {
    private lazy val entity = person(name).withId(name)
    def changeTown(town: String): Saga[Unit] =
      entity.emit(PersonEvent(name.some, town.some))

    def get(): Saga[Person] = entity.getState()
  }

  val bob = PersonEntity("bob")
  val jerry = PersonEntity("jerry")
  val alice = PersonEntity("alice")

  val saga = for {
    _ <- bob.changeTown("Portland")
    _ <- jerry.changeTown("Seattle")
    _ <- bob.changeTown("New York City")
    bobState <- bob.get()
    jerryState <- jerry.get()
    _ = println(bobState)
    _ = println(jerryState)
    aliceState <- alice.get()
    _ = println(aliceState)
  } yield ()

  val eventStore: EventStoreReader[IO] = new EventStoreReader[IO] {
    def getEvents(stream: String): IO[List[Event]] = IO(List.empty)

    def getEventNumber(stream: String): IO[Int] = IO(0)
  }

  def run: IO[Unit] =
    saga.createDelta[IO](eventStore, EventStoreDelta.EventCache.empty)
      .map(_ => ())
}
