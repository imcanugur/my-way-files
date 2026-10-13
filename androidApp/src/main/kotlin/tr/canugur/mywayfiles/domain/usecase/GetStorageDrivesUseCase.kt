package tr.canugur.mywayfiles.domain.usecase

import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.model.StorageDriveType
import tr.canugur.mywayfiles.domain.repository.StorageRepository

/**
 * UseCase to retrieve connected storage drives, sorted with primary internal drives first,
 * followed by SD cards, and finally external USB drives.
 */
class GetStorageDrivesUseCase(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(): List<StorageDrive> {
        return repository.getStorageDrives()
            .sortedWith(
                compareBy<StorageDrive> { drive ->
                    when (drive.type) {
                        StorageDriveType.INTERNAL -> 0
                        StorageDriveType.SD_CARD -> 1
                        StorageDriveType.USB -> 2
                    }
                }.thenBy { it.name }
            )
    }
}
