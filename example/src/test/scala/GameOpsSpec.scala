import GameOps._
import cats._
import cats.data._
import cats.effect._
import cats.effect.std._
import cats.mtl.Ask
import cats.mtl.Tell
import cats.syntax.all._
import example.events._
import example.states._
import smithy4s.Timestamp

import java.nio.charset.Charset

class GameOpsSpec extends munit.CatsEffectSuite {
  import GameOpsSpec.*

  test("view shop inventory") {
    type F = StateT[IO, TestConsoleState, _]

    val state = Game.initialized(
      Initialized(
        Timestamp(2025, 3, 16, 17, 39, 0, 0),
        Timestamp(2025, 3, 16, 17, 45, 0, 0),
        100,
        Cafe(
          Nil,
          0
        ),
        Shop(
          Timestamp(2025, 3, 16, 17, 45, 0, 0),
          List(
            PinballTable(
              1,
              "Star Wars",
              "Stern",
              75,
              60,
              List(Score("BRT", 99_500_000L))
            )
          )
        )
      )
    )

    val expected = TestConsoleState(
      Nil,
      """Welcome to Odin's Arcade Shop!
        |
        |We have 1 tables available for sale.
        |
        |A Star Wars.
        |Manufactured by Stern.
        |Looks to be at 60%.
        |
        |""".stripMargin,
      ""
    )

    val result = runStep[IO](
      runViewShopInventory[F],
      state
    )

    result.assertEquals(expected)
  }

  test("view cafe") {
    type F = StateT[IO, TestConsoleState, _]

    val state = Game.initialized(
      Initialized(
        Timestamp(2025, 3, 16, 17, 39, 0, 0),
        Timestamp(2025, 3, 16, 17, 45, 0, 0),
        100,
        Cafe(
          List(
            PinballTable(
              1,
              "Venom",
              "Stern",
              100,
              90,
              Nil
            ),
            PinballTable(
              2,
              "Iron Man",
              "Stern",
              50,
              30,
              Nil
            )
          ),
          0
        ),
        Shop(
          Timestamp(2025, 3, 16, 17, 45, 0, 0),
          Nil
        )
      )
    )

    val expected = TestConsoleState(
      Nil,
      """Pincafe!
        |
        |You have 2 tables on the floor.
        |
        |A Venom.
        |Manufactured by Stern.
        |Looks to be at 90%.
        |
        |A Iron Man.
        |Manufactured by Stern.
        |Looks to be at 30%.
        |
        |""".stripMargin,
      ""
    )

    val result = runStep(
      runViewCafe[F],
      state
    )

    result.assertEquals(expected)
  }
}

object GameOpsSpec {
  final case class TestConsoleState(
      input: List[String],
      output: String,
      error: String
  )

  given [F[_]: Monad]: Console[StateT[F, TestConsoleState, _]] =
    new Console[StateT[F, TestConsoleState, _]] {
      override def readLineWithCharset(
          charset: Charset
      ): StateT[F, TestConsoleState, String] =
        StateT(state =>
          Monad[F].pure(
            state.copy(input = state.input.tail),
            state.input.headOption.getOrElse("")
          )
        )

      override def print[A](a: A)(using
          S: Show[A]
      ): StateT[F, TestConsoleState, Unit] =
        StateT.modifyF(state =>
          Monad[F].pure(state.copy(output = state.output + a.show))
        )

      override def println[A](a: A)(using
          S: Show[A]
      ): StateT[F, TestConsoleState, Unit] = print(a.show + "\n")

      override def error[A](a: A)(using
          S: Show[A]
      ): StateT[F, TestConsoleState, Unit] = StateT.modifyF(state =>
        Monad[F].pure(state.copy(error = state.error + a.show))
      )

      override def errorln[A](a: A)(using
          S: Show[A]
      ): StateT[F, TestConsoleState, Unit] = error(a.show + "\n")
    }

  def askForStaticState[F[_]: Monad](
      state: Game
  ): Ask[StateT[F, TestConsoleState, _], Game] =
    new Ask[StateT[F, TestConsoleState, _], Game] {
      override def applicative: Applicative[StateT[F, TestConsoleState, _]] =
        summon[Applicative[StateT[F, TestConsoleState, _]]]

      override def ask[E2 >: Game]: StateT[F, TestConsoleState, E2] =
        StateT.pure(state)
    }

  def noChangesTell[F[_]: Monad]
      : Tell[StateT[F, TestConsoleState, _], GameEvent] =
    new Tell[StateT[F, TestConsoleState, _], GameEvent] {

      override def functor: Functor[StateT[F, TestConsoleState, _]] =
        summon[Functor[StateT[F, TestConsoleState, _]]]

      override def tell(l: GameEvent): StateT[F, TestConsoleState, Unit] =
        StateT.pure(())
    }

  def runStep[F[_]: Monad](
      f: (
          Tell[StateT[F, TestConsoleState, _], GameEvent],
          Ask[StateT[F, TestConsoleState, _], Game],
          Console[StateT[F, TestConsoleState, _]]
      ) ?=> StateT[F, TestConsoleState, Unit],
      state: Game,
      inputs: String*
  ): F[TestConsoleState] = {
    given Tell[StateT[F, TestConsoleState, _], GameEvent] = noChangesTell
    given Ask[StateT[F, TestConsoleState, _], Game] =
      askForStaticState(state)

    f.runS(TestConsoleState(inputs.toList, "", ""))
  }
}
