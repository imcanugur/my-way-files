package tr.canugur.mywayfiles.data.datasource

import tr.canugur.mywayfiles.domain.model.FileItem

/**
 * Data source abstraction for disk operations and permissions.
 */
interface FileLocalDataSource {
    suspend fun listFiles(path: String, showHidden: Boolean = false): Result<List<FileItem>>
    fun checkStoragePermission(): Boolean
    suspend fun delete(path: String): Result<Unit>
    suspend fun rename(path: String, newName: String): Result<Unit>
    suspend fun copy(sourcePath: String, targetDirPath: String): Result<Unit>
    suspend fun move(sourcePath: String, targetDirPath: String): Result<Unit>
    suspend fun createFolder(parentDirPath: String, folderName: String): Result<Unit>
}
