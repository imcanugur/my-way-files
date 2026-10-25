package tr.canugur.mywayfiles.core.util

import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.FileItem

/**
 * Supported file categories for discovery, filtering, and icon assignment.
 */
enum class FileCategory(val labelRes: Int, val iconRes: Int) {
    ALL(R.string.filter_all, R.drawable.ic_folder),
    IMAGES(R.string.filter_images, R.drawable.ic_image),
    VIDEOS(R.string.filter_videos, R.drawable.ic_video),
    AUDIO(R.string.filter_audio, R.drawable.ic_audio),
    DOCUMENTS(R.string.filter_documents, R.drawable.ic_document),
    ARCHIVES(R.string.filter_archives, R.drawable.ic_archive),
    APKS(R.string.filter_apks, R.drawable.ic_apk)
}

/**
 * Utility to identify MIME types and file categories strictly based on extension.
 */
object FileTypeClassifier {

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg"
    )

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "mov", "avi", "3gp", "webm", "flv", "m4v", "wmv", "ts"
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "ogg", "m4a", "flac", "aac", "opus", "wma", "mid", "midi", "amr"
    )

    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "csv", "epub"
    )

    private val ARCHIVE_EXTENSIONS = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz"
    )

    private val APK_EXTENSIONS = setOf(
        "apk", "xapk", "apks"
    )

    fun isImage(extension: String): Boolean = extension.lowercase() in IMAGE_EXTENSIONS
    fun isImage(item: FileItem): Boolean = !item.isDirectory && isImage(item.extension)

    fun isVideo(extension: String): Boolean = extension.lowercase() in VIDEO_EXTENSIONS
    fun isVideo(item: FileItem): Boolean = !item.isDirectory && isVideo(item.extension)

    fun isAudio(extension: String): Boolean = extension.lowercase() in AUDIO_EXTENSIONS
    fun isAudio(item: FileItem): Boolean = !item.isDirectory && isAudio(item.extension)

    fun isDocument(extension: String): Boolean = extension.lowercase() in DOCUMENT_EXTENSIONS
    fun isDocument(item: FileItem): Boolean = !item.isDirectory && isDocument(item.extension)

    fun isArchive(extension: String): Boolean = extension.lowercase() in ARCHIVE_EXTENSIONS
    fun isArchive(item: FileItem): Boolean = !item.isDirectory && isArchive(item.extension)

    fun isApk(extension: String): Boolean = extension.lowercase() in APK_EXTENSIONS
    fun isApk(item: FileItem): Boolean = !item.isDirectory && isApk(item.extension)

    fun categorize(item: FileItem): FileCategory {
        if (item.isDirectory) return FileCategory.ALL
        val ext = item.extension.lowercase()
        return when {
            ext in IMAGE_EXTENSIONS -> FileCategory.IMAGES
            ext in VIDEO_EXTENSIONS -> FileCategory.VIDEOS
            ext in AUDIO_EXTENSIONS -> FileCategory.AUDIO
            ext in DOCUMENT_EXTENSIONS -> FileCategory.DOCUMENTS
            ext in ARCHIVE_EXTENSIONS -> FileCategory.ARCHIVES
            ext in APK_EXTENSIONS -> FileCategory.APKS
            else -> FileCategory.ALL
        }
    }

    fun matchesCategory(item: FileItem, category: FileCategory): Boolean {
        return when (category) {
            FileCategory.ALL -> true
            FileCategory.IMAGES -> isImage(item)
            FileCategory.VIDEOS -> isVideo(item)
            FileCategory.AUDIO -> isAudio(item)
            FileCategory.DOCUMENTS -> isDocument(item)
            FileCategory.ARCHIVES -> isArchive(item)
            FileCategory.APKS -> isApk(item)
        }
    }
}
