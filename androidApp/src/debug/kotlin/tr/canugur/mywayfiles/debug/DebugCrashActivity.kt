package tr.canugur.mywayfiles.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Displays the last uncaught debug exception and an ADB command to retrieve it. */
class DebugCrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = runCatching { java.io.File(filesDir, "last_crash.txt").readText() }
            .getOrElse { "Crash report could not be read. Check adb logcat for the full exception." }
        val adbCommand = "adb shell run-as $packageName cat files/last_crash.txt"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF8F9FA.toInt())
        }
        root.addView(TextView(this).apply {
            text = "MyWayFiles stopped (debug)"
            textSize = 22f
            setTextColor(0xFF202124.toInt())
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "The crash details are saved on this device. Copy them here, or retrieve the report with ADB."
            textSize = 15f
            setTextColor(0xFF5F6368.toInt())
            setPadding(0, 12, 0, 16)
        })
        root.addView(Button(this).apply {
            text = "Copy crash report"
            setOnClickListener {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("MyWayFiles crash report", report))
                text = "Copied"
            }
        })
        root.addView(TextView(this).apply {
            text = "ADB: $adbCommand"
            textSize = 13f
            setTextColor(0xFF1A73E8.toInt())
            setPadding(0, 12, 0, 12)
        })
        val details = TextView(this).apply {
            text = report
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextColor(0xFF202124.toInt())
            setPadding(12, 12, 12, 12)
            setBackgroundColor(0xFFFFFFFF.toInt())
            setTextIsSelectable(true)
        }
        root.addView(ScrollView(this).apply {
            addView(details)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(root)
    }
}
