package tr.canugur.mywayfiles.data.repository

import tr.canugur.mywayfiles.data.cache.DirectoryCache
import tr.canugur.mywayfiles.data.cache.DirectoryContentsCache
import tr.canugur.mywayfiles.data.datasource.FileLocalDataSource
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.repository.FileRepository

class FileRepositoryImpl(
    private val localDataSource: FileLocalDataSource,
    private val directoryCache: DirectoryContentsCache? = null
) : FileRepository {

    override suspend fun getCachedContents(path: String, showHidden: Boolean): List<FileItem>? {
        return directoryCache?.getMemory(path, showHidden) ?: directoryCache?.get(path, showHidden)
    }

    override suspend fun getDirectoryContents(
        path: String,
        showHidden: Boolean,
        forceRefresh: Boolean
    ): Result<List<FileItem>> {
        // The filesystem is authoritative. Cache is only a provisional render source for the UI;
        // every repository query reconciles it with current accessible storage.
        val generation = directoryCache?.beginRead(path)
        val result = localDataSource.listFiles(path, showHidden = true)
        return result.map { allItems ->
            if (generation != null) directoryCache?.put(path, allItems, generation)
            if (showHidden) allItems else allItems.filter { !it.isHidden }
        }
    }

    override fun hasStoragePermission(): Boolean {
        return localDataSource.checkStoragePermission()
    }

    override suspend fun delete(path: String): Result<Unit> {
        val result = localDataSource.delete(path)
        result.onSuccess { directoryCache?.invalidate(path) }
        return result
    }

    override suspend fun rename(path: String, newName: String): Result<Unit> {
        val result = localDataSource.rename(path, newName)
        result.onSuccess { directoryCache?.invalidate(path) }
        return result
    }

    override suspend fun copy(sourcePath: String, targetDirPath: String): Result<Unit> {
        val result = localDataSource.copy(sourcePath, targetDirPath)
        result.onSuccess { directoryCache?.invalidate(targetDirPath) }
        return result
    }

    override suspend fun move(sourcePath: String, targetDirPath: String): Result<Unit> {
        val result = localDataSource.move(sourcePath, targetDirPath)
        result.onSuccess {
            directoryCache?.invalidate(sourcePath)
            directoryCache?.invalidate(targetDirPath)
        }
        return result
    }

    override suspend fun createFolder(parentDirPath: String, folderName: String): Result<Unit> {
        val result = localDataSource.createFolder(parentDirPath, folderName)
        result.onSuccess { directoryCache?.invalidate(parentDirPath) }
        return result
    }
}
