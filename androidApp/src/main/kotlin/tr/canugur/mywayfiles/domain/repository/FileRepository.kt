package tr.canugur.mywayfiles.domain.repository

import tr.canugur.mywayfiles.domain.model.FileItem

interface FileRepository {
    suspend fun getDirectoryContents(path: String): Result<List<FileItem>>
    fun hasStoragePermission(): Boolean
}
