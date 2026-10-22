package tr.canugur.mywayfiles.data.repository

import android.content.Context
import android.os.Environment
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

    private val trashRoot: File
        get() {
            val extDir = Environment.getExternalStorageDirectory()
            if (extDir != null && extDir.exists()) {
                val candidate = File(extDir, ".myway_trash")
                if (candidate.exists() || candidate.mkdirs()) {
                    return candidate
                }
            }
            val fallback = File(context.filesDir, ".myway_trash")
            if (!fallback.exists()) fallback.mkdirs()
            return fallback
        }

    private val allTrashRoots: List<File>
        get() {
            val roots = LinkedHashSet<File>()
            val extDir = Environment.getExternalStorageDirectory()
            if (extDir != null && extDir.exists()) {
                val candidate = File(extDir, ".myway_trash")
                if (candidate.exists() || candidate.mkdirs()) {
                    roots.add(candidate)
                }
            }
            val internal = File(context.filesDir, ".myway_trash")
            if (internal.exists()) {
                roots.add(internal)
            }
            if (roots.isEmpty()) {
                if (!internal.exists()) internal.mkdirs()
                roots.add(internal)
            }
            return roots.toList()
        }

    private val filesDir: File
        get() {
            val dir = File(trashRoot, "files")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val manifestFile: File
        get() = File(trashRoot, "manifest.json")

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

    override suspend fun moveToTrash(item: FileItem): Result<Unit> = withContext(Dispatchers.IO) {
        val result = synchronized(this@RecycleBinRepositoryImpl) {
            try {
                val source = File(item.path)
                if (!source.exists()) {
                    return@withContext Result.failure(FileNotFoundException("Source not found: ${item.path}"))
                }

                val id = UUID.randomUUID().toString()
                val trashFile = File(filesDir, "${id}_${item.name}")

                var moved = source.renameTo(trashFile)
                if (!moved) {
                    if (source.isDirectory) {
                        source.copyRecursively(trashFile, overwrite = false)
                        if (!trashFile.exists()) return@withContext Result.failure(IOException("Trash copy was incomplete"))
                        if (!source.deleteRecursively()) {
                            trashFile.deleteRecursively()
                            return@withContext Result.failure(IOException("Copied ${item.name}, but could not remove the original"))
                        }
                    } else {
                        source.copyTo(trashFile, overwrite = false)
                        if (!trashFile.isFile || trashFile.length() != source.length()) {
                            trashFile.delete()
                            return@withContext Result.failure(IOException("Trash copy was incomplete"))
                        }
                        if (!source.delete()) {
                            trashFile.delete()
                            return@withContext Result.failure(IOException("Copied ${item.name}, but could not remove the original"))
                        }
                    }
                    moved = trashFile.exists()
                }

                if (!moved || !trashFile.exists()) {
                    return@withContext Result.failure(IOException("Failed to move ${item.name} to trash"))
                }

                val trashedItem = TrashedItem(
                    id = id,
                    name = item.name,
                    trashPath = trashFile.canonicalPath,
                    originalPath = source.canonicalPath,
                    deletedAtMs = System.currentTimeMillis(),
                    sizeBytes = if (item.isDirectory) computeDirectorySize(trashFile) else trashFile.length(),
                    isDirectory = item.isDirectory
                )

                val list = readManifest().toMutableList()
                list.add(trashedItem)
                writeManifest(list)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        if (result.isSuccess) directoryCache.invalidate(item.path)
        result
    }

    override suspend fun restore(trashedItem: TrashedItem): Result<Unit> = withContext(Dispatchers.IO) {
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
                        trashFile.copyTo(dest, overwrite = false)
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
            val file = File(root, "manifest.json")
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
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("trashPath", item.trashPath)
                put("originalPath", item.originalPath)
                put("deletedAtMs", item.deletedAtMs)
                put("sizeBytes", item.sizeBytes)
                put("isDirectory", item.isDirectory)
            }
            array.put(obj)
        }
        val content = array.toString(2)
        allTrashRoots.forEach { root ->
            if (!root.exists() && !root.mkdirs()) throw IOException("Unable to access the recycle bin")
            val destination = File(root, "manifest.json")
            val temporary = File(root, "manifest.json.tmp")
            val backup = File(root, "manifest.json.bak")
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

    companion object {
        @Volatile
        private var instance: RecycleBinRepositoryImpl? = null

        fun getInstance(context: Context): RecycleBinRepositoryImpl =
            instance ?: synchronized(this) {
                instance ?: RecycleBinRepositoryImpl(context.applicationContext).also { instance = it }
            }
    }
}
