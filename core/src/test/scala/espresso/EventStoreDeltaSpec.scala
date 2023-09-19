package espresso

import cats.Id
import cats.syntax.all.*
import espresso.EventStoreDelta.EventCache

class EventStoreDeltaSpec extends munit.FunSuite {

  import EventStoreDeltaSpec._
  
  test("sample program") {
    val program = for {
      x <- 1.pure[EventStoreDeltaPure]
      _ <- EventStoreDelta.modifyCache[Id](_.addEvent("test", Event(1, "data")))
    } yield x + 1

    val result: (EventCache, Int) = program.apply(mockEventStore, EventStoreDelta.EventCache.empty)

    assertEquals(
      result,
      (EventCache(Map.empty, Map("test" -> List(Event(1, "data")))), 2)
    )
  }
}

object EventStoreDeltaSpec {

  type EventStoreDeltaPure[A] = EventStoreDelta[Id, A]

  val mockEventStore: EventStoreReader[Id] = new EventStoreReader[Id] {
    def getEventNumber(stream: String): Id[Int] = 0

    def getEvents(stream: String): Id[List[Event]] = List.empty
  }
}
