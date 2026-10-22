package tr.canugur.mywayfiles.data.preferences

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Small persistent navigation metadata store. Physical file contents remain filesystem-authoritative. */
class SavedLocationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val recentFolderLimit: Int
        get() = prefs.getInt(KEY_RECENT_FOLDER_LIMIT, DEFAULT_RECENT_FOLDER_LIMIT)
            .takeIf { it in RECENT_FOLDER_LIMITS }
            ?: DEFAULT_RECENT_FOLDER_LIMIT

    @Synchronized
    fun recordVisit(path: String, name: String, isDirectory: Boolean, storageRoot: String, storageName: String) {
        if (!isDirectory || path == storageRoot || !File(path).isDirectory) return
        val updated = limitRecentFoldersPerStorage(
            removeOverlappingRecentFolders(
                listOf(SavedLocation(path, name, true, storageRoot, storageName)) +
                read(KEY_RECENTS).filterNot { previous ->
                    previous.storageRoot == storageRoot && pathsOverlap(previous.path, path)
                }
            ),
            MAX_RECENTS_PER_STORAGE
        )
        write(KEY_RECENTS, updated)
    }

    @Synchronized
    fun recentFolders(): List<SavedLocation> {
        val valid = prune(KEY_RECENTS, directoriesOnly = true)
        val limited = limitRecentFoldersPerStorage(
            removeOverlappingRecentFolders(valid),
            MAX_RECENTS_PER_STORAGE
        )
        if (limited != valid) write(KEY_RECENTS, limited)
        return limited
    }

    @Synchronized
    fun setRecentFolderLimit(limit: Int) {
        val supportedLimit = RECENT_FOLDER_LIMITS.minBy { kotlin.math.abs(it - limit) }
        prefs.edit().putInt(KEY_RECENT_FOLDER_LIMIT, supportedLimit).apply()
    }

    @Synchronized
    fun favorites(): List<SavedLocation> = prune(KEY_FAVORITES, directoriesOnly = false)

    @Synchronized
    fun isFavorite(path: String): Boolean = read(KEY_FAVORITES).any { it.path == path }

    @Synchronized
    fun toggleFavorite(path: String, name: String, isDirectory: Boolean): Boolean {
        val current = read(KEY_FAVORITES).toMutableList()
        val existing = current.indexOfFirst { it.path == path }
        if (existing >= 0) {
            current.removeAt(existing)
            write(KEY_FAVORITES, current)
            return false
        }
        if (!File(path).exists()) return false
        current.add(0, SavedLocation(path, name, isDirectory))
        write(KEY_FAVORITES, current.distinctBy { it.path }.take(MAX_FAVORITES))
        return true
    }

    @Synchronized
    fun removeFavorite(path: String) {
        write(KEY_FAVORITES, read(KEY_FAVORITES).filterNot { it.path == path })
    }

    private fun prune(key: String, directoriesOnly: Boolean): List<SavedLocation> {
        val stored = read(key)
        val valid = stored.filter { location ->
            val file = File(location.path)
            val hasRequiredStorage = key != KEY_RECENTS || !location.storageRoot.isNullOrBlank()
            hasRequiredStorage && file.exists() && (!directoriesOnly || file.isDirectory) &&
                file.isDirectory == location.isDirectory
        }
        if (valid != stored) write(key, valid)
        return valid
    }

    private fun read(key: String): List<SavedLocation> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            SavedLocation(
                path = item.getString("path"),
                name = item.getString("name"),
                isDirectory = item.getBoolean("directory"),
                storageRoot = item.optString("storageRoot").takeIf { it.isNotBlank() },
                storageName = item.optString("storageName").takeIf { it.isNotBlank() }
            )
        }
    }.getOrDefault(emptyList())

    private fun write(key: String, locations: List<SavedLocation>) {
        val array = JSONArray()
        locations.forEach { location ->
            array.put(
                JSONObject()
                    .put("path", location.path)
                    .put("name", location.name)
                    .put("directory", location.isDirectory)
                    .put("storageRoot", location.storageRoot)
                    .put("storageName", location.storageName)
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun limitRecentFoldersPerStorage(
        locations: List<SavedLocation>,
        maxPerStorage: Int
    ): List<SavedLocation> {
        val countsByStorage = mutableMapOf<String, Int>()
        return locations.distinctBy { it.path }.filter { location ->
            val storageRoot = location.storageRoot ?: return@filter false
            val count = countsByStorage[storageRoot] ?: 0
            if (count >= maxPerStorage) {
                false
            } else {
                countsByStorage[storageRoot] = count + 1
                true
            }
        }.take(MAX_RECENTS_TOTAL)
    }

    /** Keep recent entries as distinct destinations, not every ancestor of the current path. */
    private fun pathsOverlap(firstPath: String, secondPath: String): Boolean =
        isSameOrDescendant(firstPath, secondPath) || isSameOrDescendant(secondPath, firstPath)

    private fun removeOverlappingRecentFolders(locations: List<SavedLocation>): List<SavedLocation> {
        val kept = mutableListOf<SavedLocation>()
        locations.distinctBy { it.path }.forEach { candidate ->
            val overlapsNewerLocation = kept.any { newer ->
                newer.storageRoot == candidate.storageRoot && pathsOverlap(newer.path, candidate.path)
            }
            if (!overlapsNewerLocation) kept += candidate
        }
        return kept
    }

    private fun isSameOrDescendant(candidate: String, parent: String): Boolean {
        val normalizedParent = parent.trimEnd(File.separatorChar)
        return candidate == normalizedParent ||
            candidate.startsWith(normalizedParent + File.separator)
    }

    companion object {
        private const val PREFS_NAME = "myway_saved_locations"
        private const val KEY_RECENTS = "recent_folders"
        private const val KEY_FAVORITES = "favorite_locations"
        private const val KEY_RECENT_FOLDER_LIMIT = "recent_folder_limit"
        private const val DEFAULT_RECENT_FOLDER_LIMIT = 3
        private const val MAX_RECENTS_PER_STORAGE = 5
        private const val MAX_RECENTS_TOTAL = 50
        val RECENT_FOLDER_LIMITS = listOf(1, 3, 5)
        private const val MAX_FAVORITES = 100
    }
}

data class SavedLocation(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val storageRoot: String? = null,
    val storageName: String? = null
)
