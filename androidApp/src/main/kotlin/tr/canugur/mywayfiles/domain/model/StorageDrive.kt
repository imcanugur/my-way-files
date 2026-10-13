package tr.canugur.mywayfiles.domain.model

/**
 * Immutable domain model representing a physical or emulated storage volume.
 */
data class StorageDrive(
    val id: String,
    val name: String,
    val path: String,
    val type: StorageDriveType,
    val totalBytes: Long,
    val freeBytes: Long,
    val isPrimary: Boolean,
    val isRemovable: Boolean
) {
    val usedBytes: Long
        get() = (totalBytes - freeBytes).coerceAtLeast(0L)

    val usageRatio: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val usagePercentage: Int
        get() = (usageRatio * 100).toInt()
}
