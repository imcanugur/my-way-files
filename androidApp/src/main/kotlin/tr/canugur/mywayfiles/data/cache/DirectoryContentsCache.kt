package tr.canugur.mywayfiles.data.cache

import tr.canugur.mywayfiles.domain.model.FileItem

/** Cache contract for provisional directory rendering; it never replaces a filesystem query. */
interface DirectoryContentsCache {
    fun getMemory(path: String, showHidden: Boolean): List<FileItem>?
    suspend fun get(path: String, showHidden: Boolean): List<FileItem>?
    suspend fun beginRead(path: String): Long
    suspend fun put(path: String, allItems: List<FileItem>, expectedGeneration: Long)
    suspend fun invalidate(path: String, includeDescendants: Boolean = true)
}
