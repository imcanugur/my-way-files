package tr.canugur.mywayfiles.domain.repository

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
    suspend fun move(sourcePath: String, targetDirPath: String): Result<Unit>
    suspend fun createFolder(parentDirPath: String, folderName: String): Result<Unit>
}
