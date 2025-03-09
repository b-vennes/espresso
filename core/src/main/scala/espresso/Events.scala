package espresso

import cats.*
import cats.data.*
import cats.mtl.*

final case class Events[A](tell: Tell[State[Chain[A], _], A]) {
  type F = State[Chain[A], _]

  def apply[B](f: Tell[State[Chain[A], _], A] ?=> B): B = {
    given Tell[State[Chain[A], _], A] = tell
    f
  }
}

object Events {
  def apply[A]: Events[A] =
    Events(
      new Tell[State[Chain[A], _], A] {
        def functor: Functor[State[Chain[A], _]] =
          summon[Functor[State[Chain[A], _]]]

        def tell(l: A): State[Chain[A], Unit] =
          State.modify(_ :+ l)
      }
    )
}
