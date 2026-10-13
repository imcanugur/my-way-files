package tr.canugur.mywayfiles.domain.model

/**
 * Aggregated metrics across all mounted storage volumes.
 */
data class StorageSummary(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usageRatio: Float,
    val driveCount: Int
) {
    companion object {
        fun from(drives: List<StorageDrive>): StorageSummary {
            val total = drives.sumOf { it.totalBytes }
            val free = drives.sumOf { it.freeBytes }
            val used = (total - free).coerceAtLeast(0L)
            val ratio = if (total > 0L) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
            return StorageSummary(
                totalBytes = total,
                usedBytes = used,
                freeBytes = free,
                usageRatio = ratio,
                driveCount = drives.size
            )
        }
    }
}
