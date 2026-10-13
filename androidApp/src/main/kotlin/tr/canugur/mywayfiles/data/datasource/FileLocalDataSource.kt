package tr.canugur.mywayfiles.data.datasource

import tr.canugur.mywayfiles.domain.model.FileItem

interface FileLocalDataSource {
    suspend fun listFiles(path: String): Result<List<FileItem>>
    fun checkStoragePermission(): Boolean
}
