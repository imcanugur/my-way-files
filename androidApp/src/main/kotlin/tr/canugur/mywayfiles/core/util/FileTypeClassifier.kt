package tr.canugur.mywayfiles.core.util

import tr.canugur.mywayfiles.domain.model.FileItem

/**
 * Utility to identify MIME types and file categories strictly based on extension.
 */
object FileTypeClassifier {

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif"
    )

    fun isImage(extension: String): Boolean {
        return extension.lowercase() in IMAGE_EXTENSIONS
    }

    fun isImage(item: FileItem): Boolean {
        return !item.isDirectory && isImage(item.extension)
    }
}
