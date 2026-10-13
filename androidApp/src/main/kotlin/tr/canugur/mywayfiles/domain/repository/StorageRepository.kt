package tr.canugur.mywayfiles.domain.repository

import tr.canugur.mywayfiles.domain.model.StorageDrive

interface StorageRepository {
    suspend fun getStorageDrives(): List<StorageDrive>
}
