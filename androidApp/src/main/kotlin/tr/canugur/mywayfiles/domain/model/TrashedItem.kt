package tr.canugur.mywayfiles.domain.model

/**
 * Immutable domain entity representing a file or directory located in the Recycle Bin.
 */
data class TrashedItem(
    val id: String,
    val name: String,
    val trashPath: String,
    val originalPath: String,
    val deletedAtMs: Long,
    val sizeBytes: Long,
    val isDirectory: Boolean
)
