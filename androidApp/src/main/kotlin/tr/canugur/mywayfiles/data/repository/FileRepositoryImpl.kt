package tr.canugur.mywayfiles.data.repository

import tr.canugur.mywayfiles.data.cache.DirectoryCache
import tr.canugur.mywayfiles.data.cache.DirectoryContentsCache
import tr.canugur.mywayfiles.data.datasource.FileLocalDataSource
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.repository.FileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque

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

    override suspend fun searchFiles(
        path: String,
        query: String,
        showHidden: Boolean,
        onResults: (List<FileItem>) -> Unit
    ): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())

        try {
            val results = mutableListOf<FileItem>()
            val pendingDirectories = ArrayDeque<File>().apply { add(File(path)) }
            val visitedDirectories = mutableSetOf<String>()
            val searchRoot = File(path).canonicalPath
            var publishedResultCount = -1

            while (pendingDirectories.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val directory = pendingDirectories.removeFirst()
                val canonicalPath = directory.canonicalPath
                if (!visitedDirectories.add(canonicalPath)) continue

                val listedChildren = localDataSource.listFiles(canonicalPath, showHidden)
                if (listedChildren.isFailure) {
                    val error = listedChildren.exceptionOrNull() ?: IllegalStateException("Unable to read directory")
                    if (error is CancellationException) throw error
                    // Storage APIs may deny access to individual subfolders (for example
                    // Android/data). Keep searching accessible siblings; only fail when
                    // the user-selected search root itself cannot be read.
                    if (canonicalPath == searchRoot) return@withContext Result.failure(error)
                    continue
                }
                val children = listedChildren.getOrThrow()
                children.forEach { child ->
                    currentCoroutineContext().ensureActive()
                    if (child.name.contains(query.trim(), ignoreCase = true)) results += child
                    if (child.isDirectory) pendingDirectories.add(File(child.path))
                }
                // Publish discovered matches after each folder so the UI can render
                // partial results while the remaining directories are scanned.
                if (results.size != publishedResultCount) {
                    publishedResultCount = results.size
                    onResults(results.toList())
                }
            }
            Result.success(results)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
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

    override suspend fun copy(sourcePath: String, targetDirPath: String): Result<Unit> =
        copy(sourcePath, targetDirPath) { _, _ -> }

    override suspend fun copy(
        sourcePath: String,
        targetDirPath: String,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> {
        val result = localDataSource.copy(sourcePath, targetDirPath, onProgress)
        result.onSuccess { directoryCache?.invalidate(targetDirPath) }
        return result
    }

    override suspend fun move(sourcePath: String, targetDirPath: String): Result<Unit> =
        move(sourcePath, targetDirPath) { _, _ -> }

    override suspend fun move(
        sourcePath: String,
        targetDirPath: String,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> {
        val result = localDataSource.move(sourcePath, targetDirPath, onProgress)
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
