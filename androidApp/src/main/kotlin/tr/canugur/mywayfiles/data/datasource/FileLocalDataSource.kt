package tr.canugur.mywayfiles.data.datasource

import tr.canugur.mywayfiles.domain.model.ConflictResolution
import tr.canugur.mywayfiles.domain.model.FileItem

/**
 * Data source abstraction for disk operations and permissions.
 */
interface FileLocalDataSource {
    suspend fun listFiles(path: String, showHidden: Boolean = false): Result<List<FileItem>>
    suspend fun listFilesInBatches(
        path: String,
        showHidden: Boolean = false,
        batchSize: Int = 128,
        onBatch: (List<FileItem>) -> Unit
    ): Result<List<FileItem>> {
        val result = listFiles(path, showHidden)
        result.onSuccess { items -> items.chunked(batchSize.coerceAtLeast(1)).forEach(onBatch) }
        return result
    }
    fun checkStoragePermission(): Boolean
    suspend fun delete(path: String): Result<Unit>
    suspend fun rename(path: String, newName: String): Result<Unit>
    suspend fun copy(sourcePath: String, targetDirPath: String): Result<Unit>
    suspend fun copy(
        sourcePath: String,
        targetDirPath: String,
        onProgress: (Long, Long) -> Unit
    ): Result<Unit> = copy(sourcePath, targetDirPath, ConflictResolution.AUTO_RENAME, onProgress)
    suspend fun copy(
        sourcePath: String,
        targetDirPath: String,
        conflictResolution: ConflictResolution,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Result<Unit> = copy(sourcePath, targetDirPath)
    suspend fun move(sourcePath: String, targetDirPath: String): Result<Unit>
    suspend fun move(
        sourcePath: String,
        targetDirPath: String,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = move(sourcePath, targetDirPath, ConflictResolution.AUTO_RENAME, onProgress)
    suspend fun move(
        sourcePath: String,
        targetDirPath: String,
        conflictResolution: ConflictResolution,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = move(sourcePath, targetDirPath)
    suspend fun createFolder(parentDirPath: String, folderName: String): Result<Unit>
}
