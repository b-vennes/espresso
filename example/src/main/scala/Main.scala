import cats.*
import cats.effect.*
import cats.data.*
import cats.mtl.*
import cats.syntax.all.*
import espresso.*
import example.events.*
import example.states.*

object Main extends IOApp.Simple {

  def newTable(
    events: Events[PinballTableEvent],
    name: String,
    manufacturer: String,
  ): Aggregate[PinballTableEvent, PinballTable] =
    Aggregate(
      events,
      PinballTable(
        name,
        manufacturer,
        100,
        100,
        List.empty
      ),
      {
        case (table, PinballTableEvent.PlayedCase(Played(score, name))) =>
          table.copy(
            currentHealth = table.currentHealth - 1,
            highScores = table.highScores :+ Score(name, score)
          )
      }
    )

  val pinballTableEvents = Events[PinballTableEvent]
  val starWarsTableAggregate = newTable(
    pinballTableEvents,
    "Star Wars",
    "Stern"
  )

  def playTable[F[_]: Monad](
    results: (Long, String)*
  )(using
    pinballTableEvents: Tell[F, PinballTableEvent]
  ): F[Unit] =
    results.traverse_(result =>
      pinballTableEvents.tell(
        PinballTableEvent.played(Played(result._1, result._2))
      )
    )

  override def run: IO[Unit] = for {
    initialEvents <- IO(Chain(
      PinballTableEvent.played(Played(1009L, "Bert"))
    ))
    result <- IO(
      pinballTableEvents(
        playTable[pinballTableEvents.F](
          2000L -> "ABC",
          44L -> "ZZZ"
        )
      )
      .productR(starWarsTableAggregate.agg)
      .runA(initialEvents)
      .value
    )
    _ <- IO.println(result)
  } yield ()
}
