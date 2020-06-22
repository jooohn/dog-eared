package me.jooohn.dogeared.di

import cats.data.StateT
import cats.{Monad, MonadError}

import scala.reflect.runtime.universe.TypeTag
import scala.util.Try

class BindT[F[_]: Monad, A] private[di] (val value: StateT[F, Memo, A]) {

  def singleton[B >: A](implicit M: MonadError[F, Throwable], tag: TypeTag[B]): BindT[F, B] =
    BindT.singleton(this)

  def map[B](f: A => B): BindT[F, B] = flatMap(a => BindT.pure(f(a)))

  def flatMap[B](f: A => BindT[F, B]): BindT[F, B] =
    new BindT(value.flatMap(f andThen (_.value)))

  def widen[B >: A]: BindT[F, B] = map(_.asInstanceOf[B])

  def compile: F[A] = Monad[F].map(value.run(Map.empty))(_._2)

}

object BindT extends BindTFunctions with BindTInstances

trait BindTFunctions {

  def pure[F[_]: Monad, A](value: A): BindT[F, A] =
    new BindT(StateT.pure(value))

  def liftF[F[_]: Monad, A](fa: F[A]): BindT[F, A] = new BindT(StateT.liftF(fa))

  def singleton[F[_]: MonadError[*[_], Throwable], A, B >: A](bindT: BindT[F, A])(
      implicit tag: TypeTag[B]): BindT[F, B] = {
    def memoize(tag: TypeTag[B])(run: StateT[F, Memo, A]): StateT[F, Memo, B] =
      for {
        memo <- StateT.get[F, Memo]
        value <- memo.get(tag) match {
          case Some(memoized) =>
            StateT.liftF[F, Memo, A](MonadError[F, Throwable].fromTry(Try(memoized.asInstanceOf[A])))
          case None =>
            for {
              newValue <- run
              _ <- StateT.modifyF[F, Memo](m =>
                m.get(tag) match {
                  case None => Monad[F].pure(m.updated(tag, newValue))
                  case Some(existing) =>
                    val message =
                      s"singleton bind for type ${tag} is declared more than twice. (existing = ${existing}, newValue = ${newValue})"
                    MonadError[F, Throwable].raiseError(new RuntimeException(message))
              })
            } yield newValue
        }
      } yield value
    new BindT(memoize(tag)(bindT.value))
  }

}

trait BindTInstances {

  implicit def catsDataMonadForBindT[F[_]: Monad]: Monad[BindT[F, *]] =
    new Monad[BindT[F, *]] {
      override def flatMap[A, B](fa: BindT[F, A])(f: A => BindT[F, B]): BindT[F, B] = fa.flatMap(f)

      override def tailRecM[A, B](
          a: A
      )(f: A => BindT[F, Either[A, B]]): BindT[F, B] =
        new BindT(Monad[StateT[F, Memo, *]].tailRecM(a)(f andThen (_.value)))

      override def pure[A](x: A): BindT[F, A] = BindT.pure(x)
    }

}
