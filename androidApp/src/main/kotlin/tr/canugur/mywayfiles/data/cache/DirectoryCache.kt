package tr.canugur.mywayfiles.data.cache

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tr.canugur.mywayfiles.domain.model.FileItem
import java.io.File

/**
 * Enterprise multi-tier directory cache:
 * - L1: Instant In-Memory Cache (0 ms, synchronous nanosecond lookup for both hidden and non-hidden states)
 * - L2: SQLite ACID Database Index (asynchronous non-blocking background persistence)
 */
class DirectoryCache(context: Context) {

    private val dbHelper = FileSystemCacheDbHelper(context)
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // L1: In-Memory Hot Cache (256 directories in RAM)
    private val memoryCache = object : LruCache<String, List<FileItem>>(256) {}

    private fun memoryKey(path: String, showHidden: Boolean): String = "$path|$showHidden"

    /**
     * Synchronous 0 ms memory lookup.
     */
    fun getMemory(path: String, showHidden: Boolean): List<FileItem>? {
        return memoryCache.get(memoryKey(path, showHidden))
    }

    /**
     * L2 lookup with L1 promotion.
     */
    suspend fun get(path: String, showHidden: Boolean): List<FileItem>? = withContext(Dispatchers.IO) {
        val key = memoryKey(path, showHidden)

        // 1. Instant L1 RAM check
        memoryCache.get(key)?.let { return@withContext it }

        // 2. L2 SQLite index check
        val items = dbHelper.getCachedItems(path, showHidden)
        if (items != null) {
            memoryCache.put(key, items)
        }
        items
    }

    /**
     * Stores both hidden and non-hidden views in L1 RAM (0 ms) and asynchronously persists
     * the complete item set to SQLite.
     */
    fun put(path: String, allItems: List<FileItem>) {
        val nonHidden = allItems.filter { !it.isHidden }
        memoryCache.put(memoryKey(path, true), allItems)
        memoryCache.put(memoryKey(path, false), nonHidden)

        // Asynchronous fire-and-forget DB persist
        backgroundScope.launch {
            val dir = File(path)
            val diskModified = if (dir.exists()) dir.lastModified() else System.currentTimeMillis()
            dbHelper.saveDirectory(path, diskModified, allItems)
        }
    }

    suspend fun invalidate(path: String) = withContext(Dispatchers.IO) {
        memoryCache.remove(memoryKey(path, true))
        memoryCache.remove(memoryKey(path, false))
        dbHelper.invalidateDirectory(path)

        val parent = File(path).parent
        if (parent != null) {
            memoryCache.remove(memoryKey(parent, true))
            memoryCache.remove(memoryKey(parent, false))
            dbHelper.invalidateDirectory(parent)
        }
    }
}
