package espresso

import cats.*
import cats.data.*
import cats.mtl.*
import cats.syntax.all.*

final case class EventsT[F[_], A](tell: Tell[StateT[F, Chain[A], _], A]) {
  type G = StateT[F, Chain[A], _]

  def apply[B](f: Tell[StateT[F, Chain[A], _], A] ?=> B): B = {
    given Tell[StateT[F, Chain[A], _], A] = tell
    f
  }
}

object EventsT {
  def apply[F[_]: Monad, A]: EventsT[F, A] =
    EventsT(
      new Tell[StateT[F, Chain[A], _], A] {
        def functor: Functor[StateT[F, Chain[A], _]] =
          summon[Functor[StateT[F, Chain[A], _]]]

        def tell(l: A): StateT[F, Chain[A], Unit] =
          StateT.modify[F, Chain[A]](_ :+ l)
      }
    )
}
