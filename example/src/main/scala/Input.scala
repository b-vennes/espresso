import cats.*
import cats.effect.std.*
import cats.parse.*
import cats.syntax.all.*
import example.actions.*
import cats.parse.Rfc5234.*

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
          .as(GameAction.debugEvents()) |
        Parser.string("purchase") *>
        whitespace.rep.void *>
        digit.rep.map(digits => digits.mkString_("").toLong)
          .map(id => GameAction.purchaseTable(PurchaseTable(id)))
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
                show"Actions are 'view shop' or 'view cafe'."
              )
              .as(None)
          case Right((_, action)) =>
            action.some.pure[F]
        }
      )
  )
}
