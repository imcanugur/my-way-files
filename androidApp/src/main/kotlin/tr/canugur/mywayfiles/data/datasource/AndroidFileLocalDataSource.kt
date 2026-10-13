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
                    // Filter out hidden system files starting with dot
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
}
