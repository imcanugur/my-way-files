package tr.canugur.mywayfiles.core.util

import tr.canugur.mywayfiles.domain.model.FileItem

/**
 * Utility to identify MIME types and file categories strictly based on extension.
 */
object FileTypeClassifier {

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif"
    )

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "mov", "avi", "3gp", "webm", "flv", "m4v", "wmv", "ts"
    )

    fun isImage(extension: String): Boolean {
        return extension.lowercase() in IMAGE_EXTENSIONS
    }

    fun isImage(item: FileItem): Boolean {
        return !item.isDirectory && isImage(item.extension)
    }

    fun isVideo(extension: String): Boolean {
        return extension.lowercase() in VIDEO_EXTENSIONS
    }

    fun isVideo(item: FileItem): Boolean {
        return !item.isDirectory && isVideo(item.extension)
    }
}
