package tr.canugur.mywayfiles.data.repository

import tr.canugur.mywayfiles.data.datasource.StorageLocalDataSource
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.repository.StorageRepository

class StorageRepositoryImpl(
    private val localDataSource: StorageLocalDataSource
) : StorageRepository {

    override suspend fun getStorageDrives(): List<StorageDrive> {
        return localDataSource.queryStorageDrives()
    }
}
