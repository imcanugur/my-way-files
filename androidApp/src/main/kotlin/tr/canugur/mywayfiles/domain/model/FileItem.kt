package tr.canugur.mywayfiles.domain.model

/**
 * Immutable domain model representing a file or directory on disk.
 */
data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val extension: String,
    val childCount: Int? = null,
    val isHidden: Boolean = false
)
