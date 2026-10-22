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

    override suspend fun listFiles(path: String, showHidden: Boolean): Result<List<FileItem>> = withContext(ioDispatcher) {
        try {
            val directory = File(path)
            if (!directory.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Path does not exist: $path"))
            }
            if (!directory.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Path is not a directory: $path"))
            }

            val files = directory.listFiles()
                ?: return@withContext Result.failure(IOException("Unable to read directory: $path"))
            val allItems = ArrayList<FileItem>(files.size)

            for (file in files) {
                try {
                    val name = file.name
                    if (name.isNullOrEmpty()) continue
                    val isDir = try { file.isDirectory } catch (_: Exception) { false }
                    val isHidden = if (name.startsWith(".")) {
                        true
                    } else if (!isDir) {
                        try { file.isHidden } catch (_: Exception) { false }
                    } else {
                        false
                    }
                    val size = if (isDir) 0L else try { file.length() } catch (_: Exception) { 0L }
                    val lastMod = try { file.lastModified() } catch (_: Exception) { 0L }
                    val ext = if (isDir) {
                        ""
                    } else {
                        val dotIdx = name.lastIndexOf('.')
                        if (dotIdx > 0 && dotIdx < name.length - 1) name.substring(dotIdx + 1).lowercase() else ""
                    }
                    val filePath = file.path

                    allItems.add(
                        FileItem(
                            id = filePath,
                            name = name,
                            path = filePath,
                            isDirectory = isDir,
                            sizeBytes = size,
                            lastModifiedMs = lastMod,
                            extension = ext,
                            childCount = null,
                            isHidden = isHidden
                        )
                    )
                } catch (_: Exception) {
                    // Skip completely inaccessible entry
                }
            }

            val resultItems = if (showHidden) allItems else allItems.filter { !it.isHidden }
            Result.success(resultItems)
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
            if (!isValidChildName(newName)) return@withContext Result.failure(IllegalArgumentException("Enter a valid file name"))
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
                if (!dest.exists()) return@withContext Result.failure(IOException("Copy did not create ${dest.name}"))
            } else {
                source.copyTo(dest, overwrite = false)
                if (!dest.isFile || dest.length() != source.length()) {
                    dest.delete()
                    return@withContext Result.failure(IOException("Copy was incomplete: ${dest.name}"))
                }
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
                    source.copyRecursively(dest, overwrite = false)
                    if (!dest.exists()) return@withContext Result.failure(IOException("Move copy was incomplete"))
                    if (!source.deleteRecursively()) {
                        dest.deleteRecursively()
                        return@withContext Result.failure(IOException("Copied ${source.name}, but could not remove the original"))
                    }
                } else {
                    source.copyTo(dest, overwrite = false)
                    if (!dest.isFile || dest.length() != source.length()) {
                        dest.delete()
                        return@withContext Result.failure(IOException("Move copy was incomplete"))
                    }
                    if (!source.delete()) {
                        dest.delete()
                        return@withContext Result.failure(IOException("Copied ${source.name}, but could not remove the original"))
                    }
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
            if (!isValidChildName(folderName)) return@withContext Result.failure(IllegalArgumentException("Enter a valid folder name"))
            val newFolder = File(parent, folderName)
            if (newFolder.exists()) return@withContext Result.failure(IOException("Folder already exists: $folderName"))
            val success = newFolder.mkdirs()
            if (success) Result.success(Unit) else Result.failure(IOException("Failed to create folder $folderName"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidChildName(name: String): Boolean =
        name.isNotBlank() && name != "." && name != ".." &&
            !name.contains('/') && !name.contains('\\') && !name.contains('\u0000')
}
