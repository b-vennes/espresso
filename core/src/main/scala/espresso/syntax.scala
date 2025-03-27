package espresso

import cats.Applicative
import cats.data.*
import cats.effect.*
import cats.effect.kernel.Unique.Token
import cats.effect.std.Random
import cats.syntax.all.*

import scala.concurrent.duration.*

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

  final class RandomStateT[F[_]: Applicative, S](using
      random: Random[F]
  ) extends Random[StateT[F, S, _]] {

    override def betweenDouble(
        minInclusive: Double,
        maxExclusive: Double
    ): StateT[F, S, Double] =
      StateT.lift(random.betweenDouble(minInclusive, maxExclusive))

    override def betweenFloat(
        minInclusive: Float,
        maxExclusive: Float
    ): StateT[F, S, Float] =
      StateT.lift(random.betweenFloat(minInclusive, maxExclusive))

    override def betweenInt(
        minInclusive: Int,
        maxExclusive: Int
    ): StateT[F, S, Int] =
      StateT.lift(random.betweenInt(minInclusive, maxExclusive))

    override def betweenLong(
        minInclusive: Long,
        maxExclusive: Long
    ): StateT[F, S, Long] =
      StateT.lift(random.betweenLong(minInclusive, maxExclusive))

    override def nextAlphaNumeric: StateT[F, S, Char] =
      StateT.lift(random.nextAlphaNumeric)

    override def nextBoolean: StateT[F, S, Boolean] =
      StateT.lift(random.nextBoolean)

    override def nextBytes(n: Int): StateT[F, S, Array[Byte]] =
      StateT.lift(random.nextBytes(n))

    override def nextDouble: StateT[F, S, Double] =
      StateT.lift(random.nextDouble)

    override def nextFloat: StateT[F, S, Float] =
      StateT.lift(random.nextFloat)

    override def nextGaussian: StateT[F, S, Double] =
      StateT.lift(random.nextGaussian)

    override def nextInt: StateT[F, S, Int] =
      StateT.lift(random.nextInt)

    override def nextIntBounded(n: Int): StateT[F, S, Int] =
      StateT.lift(random.nextIntBounded(n))

    override def nextLong: StateT[F, S, Long] =
      StateT.lift(random.nextLong)

    override def nextLongBounded(n: Long): StateT[F, S, Long] =
      StateT.lift(random.nextLongBounded(n))

    override def nextPrintableChar: StateT[F, S, Char] =
      StateT.lift(random.nextPrintableChar)

    override def nextString(length: Int): StateT[F, S, String] =
      StateT.liftF(random.nextString(length))

    override def shuffleList[A](l: List[A]): StateT[F, S, List[A]] =
      StateT.lift(random.shuffleList(l))

    override def shuffleVector[A](v: Vector[A]): StateT[F, S, Vector[A]] =
      StateT.lift(random.shuffleVector(v))
  }

  given [F[_]: Applicative, S](using Random[F]): Random[StateT[F, S, _]] =
    new RandomStateT[F, S]
}
