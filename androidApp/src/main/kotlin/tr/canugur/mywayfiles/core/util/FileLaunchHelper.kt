package tr.canugur.mywayfiles.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.FileItem
import java.io.File

/**
 * Universal file launcher using Android FileProvider and system Intent.ACTION_VIEW.
 * Safely resolves viewer apps for all file formats (PDF, audio, documents, APK, archives)
 * with graceful failure feedback.
 */
object FileLaunchHelper {

    fun openFile(context: Context, fileItem: FileItem): Boolean {
        return openFile(context, fileItem.path, fileItem.extension)
    }

    fun openFile(context: Context, filePath: String, extension: String = ""): Boolean {
        val file = File(filePath)
        if (!file.exists()) return false

        val ext = extension.ifEmpty { file.extension }.lowercase()
        val mimeType = getMimeType(ext)

        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                val chooser = Intent.createChooser(intent, file.name).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (_: Exception) {
                Toast.makeText(context, R.string.cannot_open_file, Toast.LENGTH_SHORT).show()
                false
            }
        } catch (_: Exception) {
            Toast.makeText(context, R.string.cannot_open_file, Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun getMimeType(extension: String): String {
        val cleanExt = extension.trimStart('.').lowercase()
        val mapped = try {
            MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(cleanExt)
        } catch (_: Throwable) {
            null
        }
        if (!mapped.isNullOrBlank()) return mapped

        return when (cleanExt) {
            "pdf" -> "application/pdf"
            "apk", "xapk", "apks" -> "application/vnd.android.package-archive"
            "epub" -> "application/epub+zip"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz", "gzip" -> "application/gzip"
            "json" -> "application/json"
            "txt", "log", "md", "cfg", "ini" -> "text/plain"
            "html", "htm" -> "text/html"
            "csv" -> "text/csv"
            "mp3", "wav", "ogg", "m4a", "flac", "aac", "opus", "wma", "mid", "midi" -> "audio/*"
            "mp4", "mkv", "mov", "avi", "3gp", "webm", "flv", "m4v", "wmv", "ts" -> "video/*"
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif" -> "image/*"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> "*/*"
        }
    }
}
