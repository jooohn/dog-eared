package me.jooohn.dogeared.di

import cats.data.StateT
import cats.{Monad, MonadError}

import scala.reflect.runtime.universe.TypeTag

class InjectT[F[_]: Monad, A] private[di] (val value: StateT[F, Memo, A]) {

  def map[B](f: A => B): InjectT[F, B] = flatMap(a => InjectT.pure(f(a)))

  def flatMap[B](f: A => InjectT[F, B]): InjectT[F, B] =
    new InjectT(value.flatMap(f andThen (_.value)))

  def always[B >: A]: BindT[F, B] = InjectT.toBindT(this).widen[B]
  def singleton[B >: A](implicit M: MonadError[F, Throwable], tag: TypeTag[B]): BindT[F, B] =
    InjectT.toBindT(this).singleton

}
object InjectT extends InjectTFunctions with InjectTInstances

trait InjectTFunctions {
  def pure[F[_]: Monad, A](value: A): InjectT[F, A] = new InjectT(StateT.pure(value))
  def liftF[F[_]: Monad, A](fa: F[A]): InjectT[F, A] = new InjectT(StateT.liftF(fa))

  private[di] def toBindT[F[_]: Monad, A](injectT: InjectT[F, A]): BindT[F, A] = new BindT(injectT.value)
  private[di] def fromBindT[F[_]: Monad, A](bindT: BindT[F, A]): InjectT[F, A] = new InjectT(bindT.value)
}

trait InjectTInstances extends LowPriorityInjectTInstances {

  implicit def injectTForBindT[F[_]: Monad, A](implicit bindT: BindT[F, A]): InjectT[F, A] =
    InjectT.fromBindT(bindT)

  implicit def catsDataMonadForInjectT[F[_]: Monad]: Monad[InjectT[F, *]] =
    new Monad[InjectT[F, *]] {
      override def flatMap[A, B](fa: InjectT[F, A])(f: A => InjectT[F, B]): InjectT[F, B] = fa.flatMap(f)

      override def tailRecM[A, B](
          a: A
      )(f: A => InjectT[F, Either[A, B]]): InjectT[F, B] =
        new InjectT(Monad[StateT[F, Memo, *]].tailRecM(a)(f andThen (_.value)))

      override def pure[A](x: A): InjectT[F, A] = InjectT.pure(x)
    }
}

trait LowPriorityInjectTInstances {
  import shapeless._

  LabelledGeneric
  MkLabelledGenericLens

  implicit def injectTForGeneric[F[_]: Monad, From, To](
      implicit G: Generic.Aux[To, From],
      B: Lazy[InjectT[F, From]]): InjectT[F, To] =
    B.value.map(G.from)

  implicit def injectTForHList[F[_], H, T <: HList](
      implicit H: Lazy[InjectT[F, H]],
      T: Lazy[InjectT[F, T]]): InjectT[F, H :: T] =
    for {
      head <- H.value
      tail <- T.value
    } yield head :: tail

  implicit def injectTForHNil[F[_]: Monad]: InjectT[F, HNil] =
    InjectT.pure[F, HNil](HNil)

}
