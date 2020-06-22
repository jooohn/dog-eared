package me.jooohn.dogeared.di

import cats.{Monad, MonadError}
import shapeless.Generic

import scala.reflect.runtime.universe._

trait DSL[F[_]] {

  type Bind[A] = BindT[F, A]
  type Inject[A] = InjectT[F, A]

  def bind[A: TypeTag](value: A)(implicit M: MonadError[F, Throwable]): Bind[A] =
    BindT.pure[F, A](value)

  def bindF[A](fa: F[A])(implicit M: Monad[F]): Bind[A] = BindT.liftF(fa)

  def inject[A](implicit instance: Inject[A]): Inject[A] = instance
  def injectF[A](fa: F[A])(implicit M: Monad[F]): Inject[A] = InjectT.liftF(fa)
  def derive[A]: PartiallyAppliedDerive[A] = new PartiallyAppliedDerive[A]

  def always[A, B >: A](inject: Inject[A])(implicit M: Monad[F]): Bind[B] = inject.always

  def singleton[A, B >: A: TypeTag](inject: Inject[A])(implicit M: MonadError[F, Throwable]): Bind[B] =
    inject.singleton

  def compile[A](implicit I: Inject[A], M: Monad[F]): F[A] = I.always.compile

  class PartiallyAppliedDerive[To] {

    def always[From](implicit generic: Generic.Aux[To, From], inject: Inject[From]): Bind[To] =
      inject.map(generic.from).always

    def singleton[From](
        implicit generic: Generic.Aux[To, From],
        inject: Inject[From],
        M: MonadError[F, Throwable],
        T: TypeTag[To]): Bind[To] =
      inject.map(generic.from).singleton
  }

}

object DSL {

  def apply[F[_]]: DSL[F] = new DSL[F] {}

}
