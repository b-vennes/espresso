import cats._
import cats.effect.std._
import cats.parse._
import cats.syntax.all._
import example.actions._

object Input {

  val whitespace: Parser[Unit] = Parser.charIn(" \t\r\n").void

  val gameActionParser: Parser[GameAction] =
    (
      Parser.string("view") *>
        whitespace.rep.void *>
        (
          Parser
            .string("shop")
            .as(GameAction.viewShopInventory()) |
            Parser
              .string("cafe")
              .as(GameAction.viewCafe())
        ) |
        Parser
          .oneOf(
            List(
              Parser.string("quit"),
              Parser.string(":q"),
              Parser.string("exit")
            )
          )
          .as(GameAction.exit()) |
        Parser.string("debug") *>
        whitespace.rep.void *>
        Parser
          .string("events")
          .as(GameAction.debugEvents())
    )

  def readAction[F[_]](using
      console: Console[F],
      F: Monad[F]
  ): F[GameAction] = F.untilDefinedM(
    console.readLine
      .flatMap(input =>
        gameActionParser
          .parse(input) match {
          case Left(error) =>
            console
              .println(
                show"Invalid action provided (parsing error is $error). Options are 'view shop' or 'view cafe'."
              )
              .as(None)
          case Right((_, action)) =>
            action.some.pure[F]
        }
      )
  )
}
