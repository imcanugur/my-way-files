package tr.canugur.mywayfiles.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Lets a developer verify the debug crash screen through adb without changing production code. */
class DebugCrashTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        throw IllegalStateException("Debug crash reporter verification")
    }
}
