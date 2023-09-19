package espresso

import smithy4s.schema.Schema
import smithy4s.schema.Field
import cats.syntax.all.*
import cats.Id
import espresso.EventStoreDelta.EventCache

class SagaSpec extends munit.FunSuite {
  import SagaSpec._
  
  test("sample program") {
    val program = for {
      apple <- ExampleEntityClass.entityType.withId("apple").pure[Saga]
      _ <- apple.emit(ExampleEventClass("red", 1))
      _ <- apple.emit(ExampleEventClass("green", 2))
      pear <- ExampleEntityClass.entityType.withId("pear").pure[Saga]
      _ <- pear.emit(ExampleEventClass("green", 3))
      _ <- pear.emit(ExampleEventClass("yellow", 4))
      appleState <- apple.getState()
      pearState <- pear.getState()
    } yield (appleState, pearState)

    val expected: (EventCache, (ExampleEntityClass, ExampleEntityClass)) =
      (
        EventCache(
          Map(
            "example_apple" -> Nil,
            "example_pear" -> Nil,
          ),
          Map(
            "example_apple" -> List(
              Event(2, """{"newS":"green","newI":2}"""),
              Event(1, """{"newS":"red","newI":1}"""),
            ),
            "example_pear" -> List(
              Event(2, """{"newS":"yellow","newI":4}"""),
              Event(1, """{"newS":"green","newI":3}"""),
            )
          )
        ),
        (
          ExampleEntityClass("green", 2),
          ExampleEntityClass("yellow", 4)
        )
      )
    

    val result: (EventCache, (ExampleEntityClass, ExampleEntityClass)) = program
      .createDelta[Id](mockEventStore, EventStoreDelta.EventCache.empty)

    assertEquals(result, expected)
  }
}

object SagaSpec {
  case class ExampleEntityClass(s: String, i: Int)

  object ExampleEntityClass {
    def default: ExampleEntityClass = ExampleEntityClass("", 0)
    val entityType = EntityType.withJsonEncoding(
      "example",
      ExampleEntityClass.default,
      (s: ExampleEntityClass, e: ExampleEventClass) =>
        ExampleEntityClass(e.newS, e.newI)
    )
  }

  case class ExampleEventClass(newS: String, newI: Int)

  object ExampleEventClass {
    implicit val schema: Schema[ExampleEventClass] =
      Schema.struct(
        Field.required("newS", Schema.string, (e: ExampleEventClass) => e.newS),
        Field.required("newI", Schema.int, (e: ExampleEventClass) => e.newI)
      )(ExampleEventClass.apply)
  }

  val mockEventStore: EventStoreReader[Id] = new EventStoreReader[Id] {
    def getEventNumber(stream: String): Id[Int] = 0
    def getEvents(stream: String): Id[List[Event]] = List.empty
  }
}
