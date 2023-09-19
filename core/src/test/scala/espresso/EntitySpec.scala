package espresso

import munit.FunSuite
import smithy4s.schema.Schema
import smithy4s.schema.Field

class EntitySpec extends FunSuite {
  import EntitySpec._

  test("Entity stream ID") {
    val entity: Entity[ExampleType, ExampleEvent] = ExampleType.entityType.withId("1234")
    val expectedId: String = "example_1234"

    assertEquals(entity.streamID, expectedId)
  }
}

object EntitySpec {
  case class ExampleType(s: String, i: Int)

  object ExampleType {
    def default: ExampleType = ExampleType("", 0)

    val entityType = EntityType.withJsonEncoding(
      "example",
      ExampleType.default,
      (s: ExampleType, e: ExampleEvent) =>
        ExampleType(e.newS, e.newI)
    )
  }

  case class ExampleEvent(newS: String, newI: Int)

  object ExampleEvent {
    implicit val schema: Schema[ExampleEvent] =
      Schema.struct(
        Field.required("newS", Schema.string, (e: ExampleEvent) => e.newS),
        Field.required("newI", Schema.int, (e: ExampleEvent) => e.newI)
      )(ExampleEvent.apply)
  }
}
