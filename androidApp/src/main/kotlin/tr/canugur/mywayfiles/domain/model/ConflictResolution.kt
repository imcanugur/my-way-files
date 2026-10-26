package tr.canugur.mywayfiles.domain.model

/**
 * Strategy for resolving name collisions during file operations (move, copy, paste).
 */
enum class ConflictResolution {
    AUTO_RENAME,
    OVERWRITE,
    SKIP,
    MERGE
}
