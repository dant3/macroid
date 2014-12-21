package macroid

import scala.language.higherKinds
import android.view.{ ViewGroup, View }
import scala.annotation.implicitNotFound
import macroid.util.Effector

@implicitNotFound("Don't know how to tweak ${W} with ${T}. Try importing an instance of CanTweak[${W}, ${T}, ...].") /** A typeclass for 'tweakable' relation */
trait CanTweak[W, T, R] {
  def tweak(w: W, t: T): Ui[R]
}

object CanTweak {
  implicit def `Widget is tweakable with Tweak`[W <: View, T <: Tweak[W]] =
    new CanTweak[W, T, W] {
      def tweak(w: W, t: T) = Ui { t(w); w }
    }

  implicit def `Widget is tweakable with Snail`[W <: View, S <: Snail[W]] =
    new CanTweak[W, S, W] {
      def tweak(w: W, s: S) = Ui { s(w); w }
    }

  implicit def `Layout is tweakable with Transformer`[L <: ViewGroup] =
    new CanTweak[L, Transformer, L] {
      def tweak(l: L, t: Transformer) = Ui { t(l); l }
    }

  implicit def `Widget is tweakable with Effector`[W <: View, F[+_], T, R](implicit effector: Effector[F], canTweak: CanTweak[W, T, R]) =
    new CanTweak[W, F[T], W] {
      def tweak(w: W, f: F[T]) = Ui { effector.foreach(f)(t ⇒ canTweak.tweak(w, t).run); w }
    }

  implicit def `Effector is tweakable`[W, F[+_], T, R](implicit effector: Effector[F], canTweak: CanTweak[W, T, R]) =
    new CanTweak[F[W], T, F[W]] {
      def tweak(f: F[W], t: T) = Ui { effector.foreach(f)(w ⇒ canTweak.tweak(w, t).run); f }
    }

  implicit def `Ui is tweakable`[W, T, R](implicit canTweak: CanTweak[W, T, R]) =
    new CanTweak[Ui[W], T, W] {
      def tweak(ui: Ui[W], t: T) = ui flatMap { w ⇒ canTweak.tweak(w, t).map(_ ⇒ w) }
    }

  implicit def `IntentWithContext is tweakable`[I <: IntentWithContext, T <: Tweak[I]] =
    new CanTweak[I, T, I] {
      def tweak(i: I, t: T): Ui[I] = Ui { t(i); i }
    }
}

/** This trait defines the tweaking operator (<~) */
private[macroid] trait Tweaking {
  /** Tweaking operator */
  implicit class TweakingOps[W](w: W) {
    /** Apply a tweak */
    def <~[T, R](t: T)(implicit canTweak: CanTweak[W, T, R]): Ui[R] = canTweak.tweak(w, t)
  }
}

object Tweaking extends Tweaking
