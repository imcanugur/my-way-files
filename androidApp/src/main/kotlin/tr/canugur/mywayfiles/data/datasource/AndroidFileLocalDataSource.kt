package tr.canugur.mywayfiles.data.datasource

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import tr.canugur.mywayfiles.domain.model.FileItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.ArrayDeque

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

    override suspend fun copy(sourcePath: String, targetDirPath: String): Result<Unit> =
        copy(sourcePath, targetDirPath) { _, _ -> }

    override suspend fun copy(
        sourcePath: String,
        targetDirPath: String,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(ioDispatcher) {
        var destination: File? = null
        try {
            val source = File(sourcePath)
            val targetDir = File(targetDirPath)
            if (!source.exists()) return@withContext Result.failure(FileNotFoundException("Source not found: $sourcePath"))
            if (!targetDir.exists() || !targetDir.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Target is not a directory"))
            }

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
            destination = dest
            val totalBytes = if (source.isDirectory) directorySize(source) else source.length()
            var copiedBytes = 0L
            var lastReportAt = SystemClock.elapsedRealtime()
            fun reportCopied(chunkBytes: Long) {
                copiedBytes += chunkBytes
                val now = SystemClock.elapsedRealtime()
                if (now - lastReportAt >= PROGRESS_REPORT_INTERVAL_MS || (totalBytes > 0L && copiedBytes >= totalBytes)) {
                    onProgress(copiedBytes.coerceAtMost(totalBytes), totalBytes)
                    lastReportAt = now
                }
            }

            if (source.isDirectory) {
                copyTreeWithProgress(source, dest, ::reportCopied)
            } else {
                copyFileWithProgress(source, dest, ::reportCopied)
                if (!dest.isFile || dest.length() != source.length()) {
                    throw IOException("Copy was incomplete: ${dest.name}")
                }
            }
            if (!dest.exists()) throw IOException("Copy did not create ${dest.name}")
            onProgress(totalBytes, totalBytes)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            destination?.takeIf { it.exists() }?.deleteRecursively()
            throw cancelled
        } catch (error: Exception) {
            destination?.takeIf { it.exists() }?.deleteRecursively()
            Result.failure(error)
        }
    }

    override suspend fun move(sourcePath: String, targetDirPath: String): Result<Unit> =
        move(sourcePath, targetDirPath) { _, _ -> }

    override suspend fun move(
        sourcePath: String,
        targetDirPath: String,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(ioDispatcher) {
        val source = File(sourcePath)
        var destination: File? = null
        try {
            if (!source.exists()) return@withContext Result.failure(FileNotFoundException("Source not found: $sourcePath"))
            val targetDir = File(targetDirPath)
            if (!targetDir.exists() || !targetDir.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Target is not a directory"))
            }

            val dest = File(targetDir, source.name)
            destination = dest
            if (dest.exists()) return@withContext Result.failure(IOException("Destination already exists: ${dest.name}"))

            // A same-volume rename is effectively instant. Cross-volume moves fall back to a
            // cancellable buffered copy that reports the bytes actually written.
            if (source.renameTo(dest)) {
                onProgress(0L, 0L)
                return@withContext Result.success(Unit)
            }

            val totalBytes = if (source.isDirectory) directorySize(source) else source.length()
            var copiedBytes = 0L
            var lastReportAt = SystemClock.elapsedRealtime()
            fun reportCopied(chunkBytes: Long) {
                copiedBytes += chunkBytes
                val now = SystemClock.elapsedRealtime()
                val completed = totalBytes > 0L && copiedBytes >= totalBytes
                if (now - lastReportAt >= PROGRESS_REPORT_INTERVAL_MS || completed) {
                    onProgress(copiedBytes.coerceAtMost(totalBytes), totalBytes)
                    lastReportAt = now
                }
            }

            if (source.isDirectory) {
                copyTreeWithProgress(source, dest, ::reportCopied)
            } else {
                copyFileWithProgress(source, dest, ::reportCopied)
                if (!dest.isFile || dest.length() != source.length()) {
                    throw IOException("Move copy was incomplete: ${dest.name}")
                }
            }
            if (!dest.exists()) throw IOException("Move copy was incomplete: ${dest.name}")
            onProgress(totalBytes, totalBytes)

            val removed = if (source.isDirectory) source.deleteRecursively() else source.delete()
            if (!removed) throw IOException("Copied ${source.name}, but could not remove the original")
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            destination?.takeIf { it.exists() }?.deleteRecursively()
            throw cancelled
        } catch (error: Exception) {
            destination?.takeIf { it.exists() }?.deleteRecursively()
            Result.failure(error)
        }
    }

    private suspend fun directorySize(root: File): Long {
        val pending = ArrayDeque<File>().apply { add(root) }
        val visited = mutableSetOf<String>()
        var total = 0L
        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val file = pending.removeLast()
            if (file.isDirectory) {
                val canonical = file.canonicalPath
                if (!visited.add(canonical)) continue
                val children = file.listFiles() ?: throw IOException("Unable to read ${file.path}")
                children.forEach { pending.add(it) }
            } else {
                total += file.length()
            }
        }
        return total
    }

    private suspend fun copyTreeWithProgress(
        source: File,
        destination: File,
        onChunk: (Long) -> Unit,
        visitedDirectories: MutableSet<String> = mutableSetOf()
    ) {
        currentCoroutineContext().ensureActive()
        if (source.isDirectory) {
            if (!visitedDirectories.add(source.canonicalPath)) return
            if (!destination.mkdirs() && !destination.isDirectory) {
                throw IOException("Unable to create ${destination.path}")
            }
            val children = source.listFiles() ?: throw IOException("Unable to read ${source.path}")
            for (child in children) {
                copyTreeWithProgress(child, File(destination, child.name), onChunk, visitedDirectories)
            }
            destination.setLastModified(source.lastModified())
        } else {
            copyFileWithProgress(source, destination, onChunk)
            if (destination.length() != source.length()) {
                throw IOException("Move copy was incomplete: ${destination.name}")
            }
            destination.setLastModified(source.lastModified())
        }
    }

    private suspend fun copyFileWithProgress(source: File, destination: File, onChunk: (Long) -> Unit) {
        val coroutineContext = currentCoroutineContext()
        destination.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) throw IOException("Unable to create ${parent.path}")
        }
        FileInputStream(source).use { input ->
            FileOutputStream(destination, false).use { output ->
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    onChunk(read.toLong())
                }
                output.flush()
            }
        }
    }

    private companion object {
        const val COPY_BUFFER_SIZE = 128 * 1024
        const val PROGRESS_REPORT_INTERVAL_MS = 100L
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
