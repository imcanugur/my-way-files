package tr.canugur.mywayfiles.data.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.model.StorageDriveType
import java.io.File

/**
 * Enterprise persistent cache for storage drive configurations and statistics.
 * Enables instantaneous zero-lag cold start for the main screen.
 */
class DriveCache(private val context: Context) {

    private val cacheFile: File
        get() = File(context.filesDir, "storage_drives_cache.json")

    @Volatile
    private var memoryCache: List<StorageDrive>? = null

    suspend fun getCachedDrives(): List<StorageDrive> = withContext(Dispatchers.IO) {
        memoryCache?.let { return@withContext it }

        val file = cacheFile
        if (!file.exists() || !file.canRead()) return@withContext emptyList()

        try {
            val content = file.readText()
            if (content.isBlank()) return@withContext emptyList()

            val jsonArray = JSONArray(content)
            val drives = mutableListOf<StorageDrive>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeName = obj.optString("type", StorageDriveType.INTERNAL.name)
                val driveType = try {
                    StorageDriveType.valueOf(typeName)
                } catch (_: Exception) {
                    StorageDriveType.INTERNAL
                }

                drives.add(
                    StorageDrive(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        path = obj.getString("path"),
                        type = driveType,
                        totalBytes = obj.getLong("totalBytes"),
                        freeBytes = obj.getLong("freeBytes"),
                        isPrimary = obj.optBoolean("isPrimary", false),
                        isRemovable = obj.optBoolean("isRemovable", false)
                    )
                )
            }
            memoryCache = drives
            drives
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveDrives(drives: List<StorageDrive>) = withContext(Dispatchers.IO) {
        if (drives.isEmpty()) return@withContext
        memoryCache = drives

        try {
            val jsonArray = JSONArray()
            for (drive in drives) {
                val obj = JSONObject().apply {
                    put("id", drive.id)
                    put("name", drive.name)
                    put("path", drive.path)
                    put("type", drive.type.name)
                    put("totalBytes", drive.totalBytes)
                    put("freeBytes", drive.freeBytes)
                    put("isPrimary", drive.isPrimary)
                    put("isRemovable", drive.isRemovable)
                }
                jsonArray.put(obj)
            }
            cacheFile.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }
}
