package tr.canugur.mywayfiles.data.datasource

import tr.canugur.mywayfiles.domain.model.StorageDrive

interface StorageLocalDataSource {
    suspend fun queryStorageDrives(): List<StorageDrive>
}
