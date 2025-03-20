package espresso

import cats.data._
import cats.effect._
import cats.effect.kernel.Unique.Token
import cats.syntax.all._

import scala.concurrent.duration._

object syntax {
  final class TemporalStateT[F[_], S](using
      F: Temporal[F]
  ) extends Temporal[StateT[F, S, _]] {

    override def pure[A](x: A): StateT[F, S, A] =
      StateT.pure(x)

    override def raiseError[A](e: Throwable): StateT[F, S, A] =
      StateT.liftF(F.raiseError[A](e))

    override def handleErrorWith[A](fa: StateT[F, S, A])(
        f: Throwable => StateT[F, S, A]
    ): StateT[F, S, A] =
      fa.handleErrorWith(f)

    override def monotonic: StateT[F, S, FiniteDuration] =
      StateT.liftF(F.monotonic)

    override def realTime: StateT[F, S, FiniteDuration] =
      StateT.liftF(F.realTime)

    override def flatMap[A, B](fa: StateT[F, S, A])(
        f: A => StateT[F, S, B]
    ): StateT[F, S, B] =
      fa.flatMap(f)

    override def tailRecM[A, B](a: A)(
        f: A => StateT[F, S, Either[A, B]]
    ): StateT[F, S, B] =
      f(a).flatMap {
        case Left(a)  => tailRecM(a)(f)
        case Right(b) => StateT.pure(b)
      }

    override def ref[A](a: A): StateT[F, S, Ref[StateT[F, S, _], A]] = ???

    override def deferred[A]: StateT[F, S, Deferred[StateT[F, S, _], A]] = ???

    override def start[A](
        fa: StateT[F, S, A]
    ): StateT[F, S, Fiber[StateT[F, S, _], Throwable, A]] = ???

    override def never[A]: StateT[F, S, A] =
      StateT.liftF(F.never)

    override def cede: StateT[F, S, Unit] =
      StateT.liftF(F.cede)

    override protected def sleep(time: FiniteDuration): StateT[F, S, Unit] =
      StateT.liftF(F.sleep(time))

    override def forceR[A, B](fa: StateT[F, S, A])(
        fb: StateT[F, S, B]
    ): StateT[F, S, B] = ???

    override def uncancelable[A](
        body: Poll[StateT[F, S, _]] => StateT[F, S, A]
    ): StateT[F, S, A] = ???

    override def canceled: StateT[F, S, Unit] = ???

    override def onCancel[A](
        fa: StateT[F, S, A],
        fin: StateT[F, S, Unit]
    ): StateT[F, S, A] = ???

    override def unique: StateT[F, S, Token] =
      StateT.liftF(F.unique)
  }

  given [F[_], S](using Temporal[F]): Temporal[StateT[F, S, _]] =
    new TemporalStateT[F, S]()
}
