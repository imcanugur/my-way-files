package tr.canugur.mywayfiles.domain.repository

import tr.canugur.mywayfiles.domain.model.ConflictResolution
import tr.canugur.mywayfiles.domain.model.FileItem

/**
 * Universal repository contract for file system queries and state mutations.
 */
interface FileRepository {
    suspend fun getDirectoryContents(
        path: String,
        showHidden: Boolean = false,
        forceRefresh: Boolean = false
    ): Result<List<FileItem>>
    suspend fun getCachedContents(path: String, showHidden: Boolean = false): List<FileItem>? = null
    suspend fun getDirectoryContentsStreaming(
        path: String,
        showHidden: Boolean = false,
        forceRefresh: Boolean = false,
        onBatch: (List<FileItem>) -> Unit
    ): Result<List<FileItem>> {
        val result = getDirectoryContents(path, showHidden, forceRefresh)
        result.onSuccess(onBatch)
        return result
    }
    /** Searches the accessible descendants of [path], including nested folders. */
    suspend fun searchFiles(
        path: String,
        query: String,
        showHidden: Boolean = false,
        onResults: (List<FileItem>) -> Unit = {}
    ): Result<List<FileItem>>
    fun hasStoragePermission(): Boolean
    suspend fun delete(path: String): Result<Unit>
    suspend fun rename(path: String, newName: String): Result<Unit>
    suspend fun copy(sourcePath: String, targetDirPath: String): Result<Unit>
    suspend fun copy(sourcePath: String, targetDirPath: String, onProgress: (Long, Long) -> Unit): Result<Unit> =
        copy(sourcePath, targetDirPath, ConflictResolution.AUTO_RENAME, onProgress)
    suspend fun copy(
        sourcePath: String,
        targetDirPath: String,
        conflictResolution: ConflictResolution,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Result<Unit> = copy(sourcePath, targetDirPath)
    suspend fun copyWithNestedConflictResolver(
        sourcePath: String,
        targetDirPath: String,
        conflictResolution: ConflictResolution,
        onProgress: (Long, Long) -> Unit,
        resolveNestedConflict: suspend (sourcePath: String, destinationPath: String, allowMergeFolders: Boolean) -> ConflictResolution
    ): Result<Unit> = copy(sourcePath, targetDirPath, conflictResolution, onProgress)
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
    suspend fun moveWithNestedConflictResolver(
        sourcePath: String,
        targetDirPath: String,
        conflictResolution: ConflictResolution,
        onProgress: (Long, Long) -> Unit,
        resolveNestedConflict: suspend (sourcePath: String, destinationPath: String, allowMergeFolders: Boolean) -> ConflictResolution
    ): Result<Unit> = move(sourcePath, targetDirPath, conflictResolution, onProgress)
    suspend fun createFolder(parentDirPath: String, folderName: String): Result<Unit>
}
