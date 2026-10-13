package tr.canugur.mywayfiles.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Enterprise crash-proof file sharing utility using Android FileProvider with fallback.
 */
object FileShareHelper {

    fun shareFile(context: Context, filePath: String) {
        val mimeType = when {
            FileTypeClassifier.isVideo(filePath) -> "video/*"
            FileTypeClassifier.isImage(filePath) -> "image/*"
            else -> "*/*"
        }

        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, null))
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))
            }
            context.startActivity(Intent.createChooser(fallbackIntent, null))
        }
    }
}
