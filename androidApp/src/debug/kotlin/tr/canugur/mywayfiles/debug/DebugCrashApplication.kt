package tr.canugur.mywayfiles.debug

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** Debug-only crash capture. Release builds do not include this source set or manifest. */
class DebugCrashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        if (currentProcessName() != packageName) return

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val handling = AtomicBoolean(false)
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            if (handling.compareAndSet(false, true)) {
                runCatching {
                    writeCrashReport(thread, error)
                    startActivity(Intent(this, DebugCrashActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun currentProcessName(): String = runCatching {
        File("/proc/${Process.myPid()}/cmdline").readText().substringBefore('\u0000')
    }.getOrDefault(packageName)

    private fun writeCrashReport(thread: Thread, error: Throwable) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
        val report = buildString {
            appendLine("MyWayFiles debug crash report")
            appendLine("Time: $time")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine("\n$error")
            error.stackTrace.forEach { appendLine("    at $it") }
            var cause = error.cause
            while (cause != null) {
                appendLine("\nCaused by: $cause")
                cause.stackTrace.forEach { appendLine("    at $it") }
                cause = cause.cause
            }
        }
        val target = File(filesDir, "last_crash.txt")
        val temporary = File(filesDir, "last_crash.txt.tmp")
        temporary.writeText(report)
        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) target.writeText(report)
    }
}
