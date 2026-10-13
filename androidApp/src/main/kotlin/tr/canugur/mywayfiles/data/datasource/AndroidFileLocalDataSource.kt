package tr.canugur.mywayfiles.data.datasource

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.canugur.mywayfiles.domain.model.FileItem
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class AndroidFileLocalDataSource(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FileLocalDataSource {

    override suspend fun listFiles(path: String): Result<List<FileItem>> = withContext(ioDispatcher) {
        try {
            val directory = File(path)
            if (!directory.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Path does not exist: $path"))
            }
            if (!directory.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Path is not a directory: $path"))
            }

            val files = directory.listFiles() ?: emptyArray()
            val items = files.mapNotNull { file ->
                try {
                    if (file.name.startsWith(".")) return@mapNotNull null

                    val isDir = file.isDirectory
                    val childCount = if (isDir) {
                        file.list()?.count { !it.startsWith(".") }
                    } else null

                    FileItem(
                        id = file.absolutePath,
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = isDir,
                        sizeBytes = if (isDir) 0L else file.length(),
                        lastModifiedMs = file.lastModified(),
                        extension = if (isDir) "" else file.extension.lowercase(),
                        childCount = childCount
                    )
                } catch (_: Exception) {
                    null
                }
            }

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            readPerm == PackageManager.PERMISSION_GRANTED
        }
    }

    override suspend fun delete(path: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext Result.failure(FileNotFoundException("File not found: $path"))
            val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (success) Result.success(Unit) else Result.failure(IOException("Failed to delete $path"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rename(path: String, newName: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext Result.failure(FileNotFoundException("File not found: $path"))
            val parent = file.parentFile ?: return@withContext Result.failure(IOException("Parent directory unavailable"))
            val target = File(parent, newName)
            if (target.exists()) return@withContext Result.failure(IOException("Destination already exists: $newName"))
            val success = file.renameTo(target)
            if (success) Result.success(Unit) else Result.failure(IOException("Failed to rename to $newName"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun copy(sourcePath: String, targetDirPath: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val source = File(sourcePath)
            val targetDir = File(targetDirPath)
            if (!source.exists()) return@withContext Result.failure(FileNotFoundException("Source not found: $sourcePath"))
            if (!targetDir.exists() || !targetDir.isDirectory) return@withContext Result.failure(IllegalArgumentException("Target is not a directory"))

            var dest = File(targetDir, source.name)
            if (dest.exists()) {
                val nameWithoutExt = source.nameWithoutExtension
                val ext = if (source.extension.isNotEmpty()) ".${source.extension}" else ""
                var counter = 1
                while (dest.exists()) {
                    dest = File(targetDir, "$nameWithoutExt ($counter)$ext")
                    counter++
                }
            }

            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = false)
            } else {
                source.copyTo(dest, overwrite = false)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun move(sourcePath: String, targetDirPath: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val source = File(sourcePath)
            val targetDir = File(targetDirPath)
            if (!source.exists()) return@withContext Result.failure(FileNotFoundException("Source not found: $sourcePath"))
            if (!targetDir.exists() || !targetDir.isDirectory) return@withContext Result.failure(IllegalArgumentException("Target is not a directory"))

            val dest = File(targetDir, source.name)
            if (dest.exists()) return@withContext Result.failure(IOException("Destination already exists: ${dest.name}"))

            val renamed = source.renameTo(dest)
            if (renamed) {
                Result.success(Unit)
            } else {
                if (source.isDirectory) {
                    source.copyRecursively(dest, overwrite = true)
                    source.deleteRecursively()
                } else {
                    source.copyTo(dest, overwrite = true)
                    source.delete()
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFolder(parentDirPath: String, folderName: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val parent = File(parentDirPath)
            if (!parent.exists() || !parent.isDirectory) return@withContext Result.failure(IllegalArgumentException("Parent is not a directory"))
            val newFolder = File(parent, folderName)
            if (newFolder.exists()) return@withContext Result.failure(IOException("Folder already exists: $folderName"))
            val success = newFolder.mkdirs()
            if (success) Result.success(Unit) else Result.failure(IOException("Failed to create folder $folderName"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
