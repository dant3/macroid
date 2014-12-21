package macroid

import android.app.{Activity, Service}
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/** Action/context bound Intent */
class IntentWithContext(intent: Intent, op: (Intent) => Any) {
  def get = intent
  def prepare = Ui {
    op(intent)
  }
}

/** This is the trait with basic methods to create new action/context bound Intent  */
private[macroid] trait IntentBuilder {
  /** creates a new Intent that will start the activity in future */
  def startActivity[T <: Activity](implicit ctx: ActivityContext):IntentWithContext = {
    val manifest = implicitly[Manifest[T]]
    new IntentWithContext(new Intent(ctx.get, manifest.getClass), ctx.get.startActivity)
  }
  /** creates a new Intent that will start a service in future */
  def startService[T <: Service](implicit ctx: AppContext):IntentWithContext = {
    val manifest = implicitly[Manifest[T]]
    new IntentWithContext(new Intent(ctx.get, manifest.getClass), ctx.get.startService)
  }
  /** creates a new Intent that will be sent as a broadcast */
  def sendBroadcast(implicit ctx: AppContext):IntentWithContext = {
    new IntentWithContext(new Intent, ctx.get.sendBroadcast)
  }
}

/** This is the trait with tweaks for bound Intent  */
private[macroid] trait IntentTweaks {
  type I = IntentWithContext

  def category(category:String*) = Tweak[I] { i =>
    for (cat <- category) i.get.addCategory(cat)
  }

  def flags(flags:Int) = Tweak[I](_.get.setFlags(flags))
  def action(action:String) = Tweak[I](_.get.setAction(action))
  def extras(extras:Bundle) = Tweak[I](_.get.putExtras(extras))
  def data(uri:Uri) = Tweak[I](_.get.setData(uri))
}

object IntentTweaks extends IntentTweaks

object IntentDsl extends IntentBuilder with IntentTweaks
