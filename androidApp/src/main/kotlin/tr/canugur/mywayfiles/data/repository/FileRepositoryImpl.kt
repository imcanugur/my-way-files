package tr.canugur.mywayfiles.data.repository

import tr.canugur.mywayfiles.data.datasource.FileLocalDataSource
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.repository.FileRepository

class FileRepositoryImpl(
    private val localDataSource: FileLocalDataSource
) : FileRepository {

    override suspend fun getDirectoryContents(path: String): Result<List<FileItem>> {
        return localDataSource.listFiles(path)
    }

    override fun hasStoragePermission(): Boolean {
        return localDataSource.checkStoragePermission()
    }
}
