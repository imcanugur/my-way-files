package tr.canugur.mywayfiles.data.cache

import android.app.ActivityManager
import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.data.preferences.AppSettingsStore
import java.io.File

/**
 * Enterprise multi-tier directory cache:
 * - L1: Bounded in-memory cache sized by file records, for provisional rendering
 * - L2: SQLite index for provisional rendering; writes and invalidations are serialized off the UI result path
 * - Filesystem queries remain authoritative, with generations preventing stale scan commits
 */
class DirectoryCache(context: Context) : DirectoryContentsCache {

    private val dbHelper = FileSystemCacheDbHelper(context)

    // The user can tune this percentage; the computed byte budget stays bounded for safety.
    @Volatile private var activeMemoryBudgetBytes = memoryCacheBudgetBytes(
        context,
        AppSettingsStore(context).directoryCacheHeapPercent(),
        AppSettingsStore(context).useExtendedFolderCache()
    )
    private val memoryCache = object : LruCache<String, List<FileItem>>(activeMemoryBudgetBytes) {
        // Approximate retained item/string memory, not just the number of rows. Cache entries
        // for hidden and visible views are charged independently, which is intentionally safe.
        override fun sizeOf(key: String, value: List<FileItem>): Int =
            value.sumOf { item ->
                128L + 2L * (item.id.length + item.name.length + item.path.length + item.extension.length)
            }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
    }
    private val mutex = Mutex()
    // The SQLite index only accelerates provisional renders; persistence must not delay a fresh listing.
    private val persistenceMutex = Mutex()
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generations = mutableMapOf<String, Long>()

    fun updateMemoryCacheSettings(context: Context, percent: Int, useExtendedHeap: Boolean) {
        val safePercent = percent.coerceIn(
            AppSettingsStore.MIN_DIRECTORY_CACHE_HEAP_PERCENT,
            AppSettingsStore.MAX_DIRECTORY_CACHE_HEAP_PERCENT
        )
        val newBudget = memoryCacheBudgetBytes(context, safePercent, useExtendedHeap)
        activeMemoryBudgetBytes = newBudget
        memoryCache.resize(newBudget)
    }

    fun currentMemoryCacheBudgetBytes(): Int = activeMemoryBudgetBytes

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
    override suspend fun put(path: String, allItems: List<FileItem>, expectedGeneration: Long) {
        // The repository passes a newly scanned list that it does not mutate; retain that
        // snapshot directly to avoid a second array allocation for very large folders.
        val snapshot = allItems
        val accepted = mutex.withLock {
            if (generations[path] != expectedGeneration) {
                false
            } else {
                memoryCache.put(memoryKey(path, true), snapshot)
                memoryCache.put(memoryKey(path, false), snapshot.filterNot { it.isHidden })
                true
            }
        }
        if (!accepted) return

        // Publish memory state first; SQLite is a disposable provisional cache and may finish later.
        persistenceScope.launch {
            persistenceMutex.withLock {
                val stillCurrent = mutex.withLock { generations[path] == expectedGeneration }
                if (stillCurrent) {
                    val directory = File(path)
                    dbHelper.saveDirectory(
                        path,
                        if (directory.exists()) directory.lastModified() else 0L,
                        snapshot
                    )
                }
            }
        }
    }

    override suspend fun invalidate(path: String, includeDescendants: Boolean) = persistenceMutex.withLock {
        mutex.withLock {
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
    }

    companion object {
        private const val BYTES_PER_MEGABYTE = 1024 * 1024
        private const val MIN_MEMORY_CACHE_BYTES = 2 * BYTES_PER_MEGABYTE
        private const val MAX_MEMORY_CACHE_BYTES = 64 * BYTES_PER_MEGABYTE

        /** Allocate a bounded share of the app heap, adapting to the device memory class. */
        fun memoryCacheBudgetBytes(
            context: Context,
            heapPercent: Int = AppSettingsStore(context).directoryCacheHeapPercent(),
            useExtendedHeap: Boolean = AppSettingsStore(context).useExtendedFolderCache()
        ): Int {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val heapClassMb = if (useExtendedHeap) activityManager?.largeMemoryClass else activityManager?.memoryClass
            val appHeapBytes = heapClassMb?.toLong()?.times(BYTES_PER_MEGABYTE.toLong())
                ?: Runtime.getRuntime().maxMemory()
            val safePercent = heapPercent.coerceIn(
                AppSettingsStore.MIN_DIRECTORY_CACHE_HEAP_PERCENT,
                AppSettingsStore.MAX_DIRECTORY_CACHE_HEAP_PERCENT
            )
            return (appHeapBytes * safePercent / 100L)
                .coerceIn(MIN_MEMORY_CACHE_BYTES.toLong(), MAX_MEMORY_CACHE_BYTES.toLong())
                .toInt()
        }

        @Volatile private var instance: DirectoryCache? = null
        fun getInstance(context: Context): DirectoryCache = instance ?: synchronized(this) {
            instance ?: DirectoryCache(context.applicationContext).also { instance = it }
        }
    }
}
