package tr.canugur.mywayfiles.data.repository

import tr.canugur.mywayfiles.data.cache.DriveCache
import tr.canugur.mywayfiles.data.datasource.StorageLocalDataSource
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.repository.StorageRepository

class StorageRepositoryImpl(
    private val localDataSource: StorageLocalDataSource,
    private val driveCache: DriveCache? = null
) : StorageRepository {

    override suspend fun getCachedStorageDrives(): List<StorageDrive> {
        return driveCache?.getCachedDrives() ?: emptyList()
    }

    override suspend fun getStorageDrives(): List<StorageDrive> {
        val drives = localDataSource.queryStorageDrives()
        if (drives.isNotEmpty()) {
            driveCache?.saveDrives(drives)
        }
        return drives
    }
}
