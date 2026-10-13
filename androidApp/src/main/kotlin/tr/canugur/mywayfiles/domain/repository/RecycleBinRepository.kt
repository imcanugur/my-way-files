package tr.canugur.mywayfiles.domain.repository

import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.TrashedItem

/**
 * Repository interface governing Recycle Bin lifecycle (soft delete, restore, purge).
 */
interface RecycleBinRepository {
    suspend fun getTrashedItems(): Result<List<TrashedItem>>
    suspend fun moveToTrash(item: FileItem): Result<Unit>
    suspend fun restore(trashedItem: TrashedItem): Result<Unit>
    suspend fun deletePermanently(trashedItem: TrashedItem): Result<Unit>
    suspend fun emptyTrash(): Result<Unit>
}
