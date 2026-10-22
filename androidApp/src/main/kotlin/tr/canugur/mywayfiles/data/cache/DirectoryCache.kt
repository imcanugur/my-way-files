package tr.canugur.mywayfiles.data.cache

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tr.canugur.mywayfiles.domain.model.FileItem
import java.io.File

/**
 * Enterprise multi-tier directory cache:
 * - L1: Instant In-Memory Cache (0 ms, synchronous nanosecond lookup for both hidden and non-hidden states)
 * - L2: SQLite index for provisional rendering; writes and invalidations are serialized
 * - Filesystem queries remain authoritative, with generations preventing stale scan commits
 */
class DirectoryCache(context: Context) : DirectoryContentsCache {

    private val dbHelper = FileSystemCacheDbHelper(context)

    // L1: In-Memory Hot Cache (256 directories in RAM)
    private val memoryCache = object : LruCache<String, List<FileItem>>(256) {}
    private val mutex = Mutex()
    private val generations = mutableMapOf<String, Long>()

    private fun memoryKey(path: String, showHidden: Boolean): String = "$path|$showHidden"

    /**
     * Synchronous 0 ms memory lookup.
     */
    override fun getMemory(path: String, showHidden: Boolean): List<FileItem>? {
        return memoryCache.get(memoryKey(path, showHidden))
    }

    override suspend fun beginRead(path: String): Long = mutex.withLock {
        val generation = (generations[path] ?: 0L) + 1L
        generations[path] = generation
        generation
    }

    /**
     * L2 lookup with L1 promotion.
     */
    override suspend fun get(path: String, showHidden: Boolean): List<FileItem>? = mutex.withLock {
        val key = memoryKey(path, showHidden)

        // 1. Instant L1 RAM check
        memoryCache.get(key)?.let { return@withLock it }

        // 2. L2 SQLite index check
        val items = dbHelper.getCachedItems(path, showHidden)
        if (items != null) {
            memoryCache.put(key, items)
        }
        items
    }

    /**
     * Stores both hidden and non-hidden views in L1 and persists the complete snapshot to SQLite.
     */
    override suspend fun put(path: String, allItems: List<FileItem>, expectedGeneration: Long) = mutex.withLock {
        if (generations[path] != expectedGeneration) return@withLock
        val nonHidden = allItems.filter { !it.isHidden }
        memoryCache.put(memoryKey(path, true), allItems)
        memoryCache.put(memoryKey(path, false), nonHidden)

        val dir = File(path)
        dbHelper.saveDirectory(path, if (dir.exists()) dir.lastModified() else 0L, allItems)
    }

    override suspend fun invalidate(path: String, includeDescendants: Boolean) = mutex.withLock {
        val normalized = path.trimEnd(File.separatorChar)
        val paths = buildSet {
            add(path)
            if (includeDescendants) {
                generations.keys.filterTo(this) { it.startsWith("$normalized${File.separator}") }
                memoryCache.snapshot().keys.map { it.substringBeforeLast('|') }
                    .filterTo(this) { it.startsWith("$normalized${File.separator}") }
            }
            File(path).parent?.let(::add)
        }
        dbHelper.invalidateDirectory(path, includeDescendants)
        paths.forEach { affected ->
            generations[affected] = (generations[affected] ?: 0L) + 1L
            memoryCache.remove(memoryKey(affected, true))
            memoryCache.remove(memoryKey(affected, false))
            if (affected != path) dbHelper.invalidateDirectory(affected)
        }
    }

    companion object {
        @Volatile private var instance: DirectoryCache? = null
        fun getInstance(context: Context): DirectoryCache = instance ?: synchronized(this) {
            instance ?: DirectoryCache(context.applicationContext).also { instance = it }
        }
    }
}
