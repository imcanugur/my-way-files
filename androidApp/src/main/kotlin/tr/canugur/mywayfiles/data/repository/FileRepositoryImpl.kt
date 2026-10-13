package tr.canugur.mywayfiles.data.repository

import tr.canugur.mywayfiles.data.datasource.FileLocalDataSource
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.repository.FileRepository

class FileRepositoryImpl(
    private val localDataSource: FileLocalDataSource
) : FileRepository {

    override suspend fun getDirectoryContents(path: String, showHidden: Boolean): Result<List<FileItem>> {
        return localDataSource.listFiles(path, showHidden)
    }

    override fun hasStoragePermission(): Boolean {
        return localDataSource.checkStoragePermission()
    }

    override suspend fun delete(path: String): Result<Unit> {
        return localDataSource.delete(path)
    }

    override suspend fun rename(path: String, newName: String): Result<Unit> {
        return localDataSource.rename(path, newName)
    }

    override suspend fun copy(sourcePath: String, targetDirPath: String): Result<Unit> {
        return localDataSource.copy(sourcePath, targetDirPath)
    }

    override suspend fun move(sourcePath: String, targetDirPath: String): Result<Unit> {
        return localDataSource.move(sourcePath, targetDirPath)
    }

    override suspend fun createFolder(parentDirPath: String, folderName: String): Result<Unit> {
        return localDataSource.createFolder(parentDirPath, folderName)
    }
}
