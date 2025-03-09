package espresso

import cats.*
import cats.data.*
import cats.mtl.*

final case class Aggregate[A, S](ask: Ask[State[Chain[A], _], S]) {
  type F = State[Chain[A], _]

  def apply[B](f: Ask[State[Chain[A], _], S] ?=> B): B = {
    given Ask[State[Chain[A], _], S] = ask
    f
  }

  def agg: State[Chain[A], S] = ask.ask
}

object Aggregate {
  def apply[A, S](
    events: Events[A],
    default: S,
    merge: (S, A) => S
  ): Aggregate[A, S] = Aggregate(
    new Ask[State[Chain[A], _], S] {
      override def applicative: Applicative[State[Chain[A], _]] =
        summon[Applicative[State[Chain[A], _]]]

      override def ask[S2 >: S]: State[Chain[A], S2] =
        State.get[Chain[A]].map(_.foldLeft(default)(merge))
    }
  )
}

