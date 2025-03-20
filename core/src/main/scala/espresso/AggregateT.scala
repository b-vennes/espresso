package espresso

import cats._
import cats.data._
import cats.mtl._

final case class AggregateT[F[_], A, S](
    ask: Ask[StateT[F, Chain[A], _], S]
) {
  type G = StateT[F, Chain[A], _]

  def apply[B](f: Ask[StateT[F, Chain[A], _], S] ?=> B): B = {
    given Ask[StateT[F, Chain[A], _], S] = ask
    f
  }

  def agg: StateT[F, Chain[A], S] = ask.ask
}

object AggregateT {
  def apply[F[_]: Monad, A, S](
      events: EventsT[F, A],
      default: S,
      merge: (S, A) => S
  ): AggregateT[F, A, S] = AggregateT(
    new Ask[StateT[F, Chain[A], _], S] {
      override def applicative: Applicative[StateT[F, Chain[A], _]] =
        summon[Applicative[StateT[F, Chain[A], _]]]

      override def ask[S2 >: S]: StateT[F, Chain[A], S2] =
        StateT.get[F, Chain[A]].map(_.foldLeft(default)(merge))
    }
  )
}
