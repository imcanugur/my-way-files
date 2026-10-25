package tr.canugur.mywayfiles.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.domain.repository.RecycleBinRepository
import tr.canugur.mywayfiles.data.cache.DirectoryCache
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

/**
 * Enterprise implementation of RecycleBinRepository with on-volume trash support,
 * instant atomic rename, and manifest persistence.
 */
class RecycleBinRepositoryImpl private constructor(
    private val context: Context
) : RecycleBinRepository {

    private val directoryCache = DirectoryCache.getInstance(context)

    private val allTrashRoots: List<File>
        get() {
            val roots = storageRoots().map { File(it, TRASH_DIRECTORY) } +
                File(context.filesDir, TRASH_DIRECTORY) // Read the legacy app-private fallback bin.
            return roots.distinctBy(::canonicalPath).filter { it.isDirectory }
        }

    /** Returns mounted storage roots that this app can address, including removable volumes. */
    private fun storageRoots(): List<File> {
        val roots = LinkedHashMap<String, File>()
        fun add(path: String?) {
            if (path.isNullOrBlank()) return
            val directory = File(path)
            if (directory.exists() && directory.isDirectory) roots[canonicalPath(directory)] = directory
        }

        add(Environment.getExternalStorageDirectory().absolutePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            manager?.storageVolumes?.forEach { volume ->
                runCatching { volume.directory?.absolutePath }.getOrNull()?.let(::add)
            }
        }
        context.getExternalFilesDirs(null).forEach { appDirectory ->
            val path = appDirectory?.absolutePath ?: return@forEach
            val appDataMarker = "${File.separator}Android${File.separator}data${File.separator}"
            val markerIndex = path.indexOf(appDataMarker)
            if (markerIndex > 0) add(path.substring(0, markerIndex))
        }
        add(context.filesDir.absolutePath)
        return roots.values.toList()
    }

    /** Find the deepest mounted volume containing the file, so trash stays on that volume. */
    private fun storageRootFor(file: File): File? {
        val path = canonicalPath(file)
        return storageRoots()
            .filter { isSameOrInside(path, canonicalPath(it)) }
            .maxByOrNull { canonicalPath(it).length }
    }

    private fun trashFilesDirectoryFor(source: File): File {
        val volumeRoot = storageRootFor(source)
            ?: throw IOException("Unable to determine the storage volume for ${source.path}")
        if (isSameOrInside(canonicalPath(source), canonicalPath(File(volumeRoot, TRASH_DIRECTORY)))) {
            throw IOException("Items already in the recycle bin cannot be moved to trash again")
        }
        val directory = File(File(volumeRoot, TRASH_DIRECTORY), TRASH_FILES_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create a recycle bin on ${volumeRoot.path}")
        }
        return directory
    }

    override suspend fun getTrashedItems(): Result<List<TrashedItem>> = withContext(Dispatchers.IO) {
        synchronized(this@RecycleBinRepositoryImpl) {
            try {
                val list = readManifest()
                val valid = list.filter { File(it.trashPath).exists() }
                if (valid.size != list.size) {
                    writeManifest(valid)
                }
                Result.success(valid.sortedByDescending { it.deletedAtMs })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun moveToTrash(item: FileItem): Result<Unit> = moveToTrash(item) { _, _ -> }

    override suspend fun moveToTrash(
        item: FileItem,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val result = synchronized(this@RecycleBinRepositoryImpl) {
            try {
                val source = File(item.path)
                if (!source.exists()) {
                    return@withContext Result.failure(FileNotFoundException("Source not found: ${item.path}"))
                }

                val id = UUID.randomUUID().toString()
                val trashFile = File(trashFilesDirectoryFor(source), "${id}_${item.name}")
                val trashedItem = TrashedItem(
                    id = id,
                    name = item.name,
                    trashPath = trashFile.canonicalPath,
                    originalPath = source.canonicalPath,
                    deletedAtMs = System.currentTimeMillis(),
                    sizeBytes = if (item.isDirectory) computeDirectorySize(source) else source.length(),
                    isDirectory = item.isDirectory
                )

                // Persist the recovery record before moving user data. If the process stops
                // between these steps, getTrashedItems() removes the pending entry because
                // its trash path does not exist and the original file remains untouched.
                val previousManifest = readManifest()
                writeManifest(previousManifest + trashedItem)

                var copyComplete = false
                try {
                    var moved = source.renameTo(trashFile)
                    if (!moved) {
                        if (source.isDirectory) {
                            source.copyRecursively(trashFile, overwrite = false)
                            if (!trashFile.exists()) throw IOException("Trash copy was incomplete")
                            copyComplete = true
                            if (!source.deleteRecursively()) {
                                throw IOException("Copied ${item.name}, but could not remove the original")
                            }
                        } else {
                            copyFileWithProgress(source, trashFile, onProgress)
                            if (!trashFile.isFile || trashFile.length() != source.length()) {
                                trashFile.delete()
                                throw IOException("Trash copy was incomplete")
                            }
                            copyComplete = true
                            if (!source.delete()) {
                                throw IOException("Copied ${item.name}, but could not remove the original")
                            }
                        }
                        moved = trashFile.exists()
                    }

                    if (!moved || !trashFile.exists()) {
                        throw IOException("Failed to move ${item.name} to trash")
                    }
                    Result.success(Unit)
                } catch (error: Exception) {
                    // A failed cross-volume copy can leave a partial destination. Remove it
                    // while keeping the original whenever possible, then discard the pending
                    // manifest entry. A later read also reconciles any failed cleanup.
                    var keepRecoveryRecord = false
                    if (trashFile.exists() && source.exists() && copyComplete) {
                        // The full copy is the recovery copy; keep its manifest entry even
                        // though removal of the source was only partially successful.
                        keepRecoveryRecord = true
                    } else if (trashFile.exists() && source.exists()) {
                        if (trashFile.isDirectory) trashFile.deleteRecursively() else trashFile.delete()
                    } else if (trashFile.exists() && !source.exists()) {
                        val restored = runCatching { trashFile.renameTo(source) }.getOrDefault(false) ||
                            runCatching {
                                if (trashFile.isDirectory) trashFile.copyRecursively(source, overwrite = false)
                                else trashFile.copyTo(source, overwrite = false)
                                source.exists()
                            }.getOrDefault(false)
                        if (restored && source.exists()) {
                            if (trashFile.isDirectory) trashFile.deleteRecursively() else trashFile.delete()
                        } else {
                            keepRecoveryRecord = true
                        }
                    }
                    if (!keepRecoveryRecord) {
                        runCatching { writeManifest(readManifest().filterNot { it.id == id }) }
                    }
                    Result.failure(error)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        if (result.isSuccess) directoryCache.invalidate(item.path)
        result
    }

    override suspend fun restore(trashedItem: TrashedItem): Result<Unit> = restore(trashedItem) { _, _ -> }

    override suspend fun restore(
        trashedItem: TrashedItem,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val result = synchronized(this@RecycleBinRepositoryImpl) {
            try {
                val trashFile = File(trashedItem.trashPath)
                if (!trashFile.exists()) {
                    return@withContext Result.failure(FileNotFoundException("Trashed file not found: ${trashedItem.trashPath}"))
                }

                var dest = File(trashedItem.originalPath)
                dest.parentFile?.mkdirs()

                if (dest.exists()) {
                    val nameWithoutExt = dest.nameWithoutExtension
                    val ext = if (dest.extension.isNotEmpty()) ".${dest.extension}" else ""
                    var counter = 1
                    while (dest.exists()) {
                        dest = File(dest.parentFile, "$nameWithoutExt ($counter)$ext")
                        counter++
                    }
                }

                var restored = trashFile.renameTo(dest)
                if (!restored) {
                    if (trashFile.isDirectory) {
                        trashFile.copyRecursively(dest, overwrite = false)
                        if (!dest.exists()) return@withContext Result.failure(IOException("Restore copy was incomplete"))
                        if (!trashFile.deleteRecursively()) {
                            dest.deleteRecursively()
                            return@withContext Result.failure(IOException("Restored ${trashedItem.name}, but could not remove the trash copy"))
                        }
                    } else {
                        copyFileWithProgress(trashFile, dest, onProgress)
                        if (!dest.isFile || dest.length() != trashFile.length()) {
                            dest.delete()
                            return@withContext Result.failure(IOException("Restore copy was incomplete"))
                        }
                        if (!trashFile.delete()) {
                            dest.delete()
                            return@withContext Result.failure(IOException("Restored ${trashedItem.name}, but could not remove the trash copy"))
                        }
                    }
                    restored = dest.exists()
                }

                if (!restored) {
                    return@withContext Result.failure(IOException("Failed to restore ${trashedItem.name}"))
                }

                val list = readManifest().filterNot { it.id == trashedItem.id }
                writeManifest(list)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        if (result.isSuccess) directoryCache.invalidate(trashedItem.originalPath)
        result
    }

    override suspend fun deletePermanently(trashedItem: TrashedItem): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(this@RecycleBinRepositoryImpl) {
            try {
                val trashFile = File(trashedItem.trashPath)
                if (trashFile.exists()) {
                    val deleted = if (trashFile.isDirectory) trashFile.deleteRecursively() else trashFile.delete()
                    if (!deleted || trashFile.exists()) return@withContext Result.failure(IOException("Unable to permanently delete ${trashedItem.name}"))
                }

                val list = readManifest().filterNot { it.id == trashedItem.id }
                writeManifest(list)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun emptyTrash(): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(this@RecycleBinRepositoryImpl) {
            try {
                val list = readManifest()
                val failures = mutableListOf<TrashedItem>()
                for (item in list) {
                    val f = File(item.trashPath)
                    if (f.exists()) {
                        val deleted = if (f.isDirectory) f.deleteRecursively() else f.delete()
                        if (!deleted || f.exists()) failures.add(item)
                    }
                }
                writeManifest(failures)
                if (failures.isEmpty()) Result.success(Unit)
                else Result.failure(IOException("Unable to permanently delete ${failures.size} item(s)"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun copyFileWithProgress(
        source: File,
        destination: File,
        onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit
    ) {
        val totalBytes = source.length()
        var copiedBytes = 0L
        var lastReportedAt = SystemClock.elapsedRealtime()
        java.io.FileInputStream(source).use { input ->
            java.io.FileOutputStream(destination, false).use { output ->
                val buffer = ByteArray(128 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copiedBytes += read
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastReportedAt >= 100L || copiedBytes >= totalBytes) {
                        onProgress(copiedBytes.coerceAtMost(totalBytes), totalBytes)
                        lastReportedAt = now
                    }
                }
                output.flush()
            }
        }
        onProgress(totalBytes, totalBytes)
    }

    private fun computeDirectorySize(dir: File): Long {
        return try {
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }

    private fun readManifest(): List<TrashedItem> {
        val results = mutableListOf<TrashedItem>()
        val seenIds = mutableSetOf<String>()
        for (root in allTrashRoots) {
            val file = File(root, MANIFEST_FILE)
            if (!file.exists()) continue
            try {
                val jsonString = file.readText()
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    if (seenIds.add(id)) {
                        results.add(
                            TrashedItem(
                                id = id,
                                name = obj.getString("name"),
                                trashPath = obj.getString("trashPath"),
                                originalPath = obj.getString("originalPath"),
                                deletedAtMs = obj.getLong("deletedAtMs"),
                                sizeBytes = obj.getLong("sizeBytes"),
                                isDirectory = obj.getBoolean("isDirectory")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                throw IOException("Unable to read recycle bin metadata", e)
            }
        }
        return results
    }

    private fun writeManifest(items: List<TrashedItem>) {
        val roots = LinkedHashMap<String, File>()
        (allTrashRoots + items.mapNotNull { item ->
            File(item.trashPath).parentFile?.parentFile
        }).forEach { root -> roots[canonicalPath(root)] = root }

        roots.values.forEach { root ->
            val filesDirectory = File(root, TRASH_FILES_DIRECTORY)
            val volumeItems = items.filter { isSameOrInside(it.trashPath, canonicalPath(filesDirectory)) }
            val array = JSONArray()
            volumeItems.forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("trashPath", item.trashPath)
                        put("originalPath", item.originalPath)
                        put("deletedAtMs", item.deletedAtMs)
                        put("sizeBytes", item.sizeBytes)
                        put("isDirectory", item.isDirectory)
                    }
                )
            }
            val content = array.toString(2)
            if (!root.exists() && !root.mkdirs()) throw IOException("Unable to access the recycle bin")
            val destination = File(root, MANIFEST_FILE)
            val temporary = File(root, "$MANIFEST_FILE.tmp")
            val backup = File(root, "$MANIFEST_FILE.bak")
            temporary.writeText(content)
            backup.delete()
            if (destination.exists() && !destination.renameTo(backup)) {
                temporary.delete()
                throw IOException("Unable to update the recycle bin index")
            }
            if (!temporary.renameTo(destination)) {
                temporary.delete()
                if (backup.exists()) backup.renameTo(destination)
                throw IOException("Unable to save the recycle bin index")
            }
            backup.delete()
        }
    }

    private fun canonicalPath(file: File): String =
        runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)

    private fun isSameOrInside(candidatePath: String, parentPath: String): Boolean {
        val normalizedParent = parentPath.trimEnd(File.separatorChar)
        return candidatePath == normalizedParent || candidatePath.startsWith(normalizedParent + File.separator)
    }

    companion object {
        private const val TRASH_DIRECTORY = ".myway_trash"
        private const val TRASH_FILES_DIRECTORY = "files"
        private const val MANIFEST_FILE = "manifest.json"

        @Volatile
        private var instance: RecycleBinRepositoryImpl? = null

        fun getInstance(context: Context): RecycleBinRepositoryImpl =
            instance ?: synchronized(this) {
                instance ?: RecycleBinRepositoryImpl(context.applicationContext).also { instance = it }
            }
    }
}
