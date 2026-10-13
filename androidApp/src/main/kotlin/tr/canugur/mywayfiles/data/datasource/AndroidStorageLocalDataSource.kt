package tr.canugur.mywayfiles.data.datasource

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.model.StorageDriveType
import java.util.Locale

class AndroidStorageLocalDataSource(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StorageLocalDataSource {

    override suspend fun queryStorageDrives(): List<StorageDrive> = withContext(ioDispatcher) {
        val drives = mutableListOf<StorageDrive>()
        val seenPaths = mutableSetOf<String>()

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager

        // 1. StorageManager API (API 24+)
        if (storageManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val volumes: List<StorageVolume> = storageManager.storageVolumes
            for (volume in volumes) {
                try {
                    val isPrimary = volume.isPrimary
                    val isRemovable = volume.isRemovable
                    val description = volume.getDescription(context)
                        ?: if (isPrimary) "Internal Storage" else "External Storage"

                    var drivePath: String? = null

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val dir = volume.directory
                        if (dir != null && dir.exists()) {
                            drivePath = dir.absolutePath
                        }
                    }

                    if (drivePath == null && isPrimary) {
                        drivePath = Environment.getExternalStorageDirectory().absolutePath
                    }

                    if (drivePath == null && volume.uuid != null) {
                        val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
                        for (extFile in externalDirs) {
                            if (extFile != null && extFile.absolutePath.contains(volume.uuid!!)) {
                                drivePath = extractVolumeRoot(extFile.absolutePath, volume.uuid!!)
                                break
                            }
                        }
                    }

                    if (drivePath != null && drivePath !in seenPaths) {
                        val (totalBytes, freeBytes) = readStatFs(drivePath, isPrimary)
                        if (totalBytes > 0L) {
                            seenPaths.add(drivePath)
                            drives.add(
                                StorageDrive(
                                    id = volume.uuid ?: "primary_internal",
                                    name = description,
                                    path = drivePath,
                                    type = resolveDriveType(isPrimary, isRemovable, description),
                                    totalBytes = totalBytes,
                                    freeBytes = freeBytes,
                                    isPrimary = isPrimary,
                                    isRemovable = isRemovable
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Suppress per-drive read failure to keep valid drives
                }
            }
        }

        // 2. Primary fallback if not discovered
        if (drives.none { it.isPrimary }) {
            try {
                val primaryDir = Environment.getExternalStorageDirectory()
                val dataDir = Environment.getDataDirectory()
                val stat = StatFs(dataDir.path)
                val total = stat.totalBytes
                val free = stat.availableBytes
                if (total > 0L) {
                    drives.add(
                        0,
                        StorageDrive(
                            id = "primary_internal",
                            name = "Internal Storage",
                            path = primaryDir.absolutePath,
                            type = StorageDriveType.INTERNAL,
                            totalBytes = total,
                            freeBytes = free,
                            isPrimary = true,
                            isRemovable = false
                        )
                    )
                    seenPaths.add(primaryDir.absolutePath)
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback scan for undetected external mounts
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            var sdIndex = 1
            for (file in externalDirs) {
                if (file != null && Environment.isExternalStorageRemovable(file)) {
                    val rootPath = file.absolutePath.substringBefore("/Android/data")
                    if (rootPath.isNotBlank() && rootPath !in seenPaths) {
                        val (total, free) = readStatFs(file.path, isPrimary = false)
                        if (total > 0L) {
                            seenPaths.add(rootPath)
                            val isUsb = rootPath.lowercase(Locale.ROOT).contains("usb")
                            val name = if (isUsb) "USB Drive" else "SD Card $sdIndex"
                            if (!isUsb) sdIndex++
                            drives.add(
                                StorageDrive(
                                    id = "ext_$rootPath",
                                    name = name,
                                    path = rootPath,
                                    type = if (isUsb) StorageDriveType.USB else StorageDriveType.SD_CARD,
                                    totalBytes = total,
                                    freeBytes = free,
                                    isPrimary = false,
                                    isRemovable = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        drives
    }

    private fun readStatFs(path: String, isPrimary: Boolean): Pair<Long, Long> {
        val targetPath = if (isPrimary) Environment.getDataDirectory().path else path
        return try {
            val stat = StatFs(targetPath)
            Pair(stat.totalBytes, stat.availableBytes)
        } catch (_: Exception) {
            try {
                val stat = StatFs(path)
                Pair(stat.totalBytes, stat.availableBytes)
            } catch (_: Exception) {
                Pair(0L, 0L)
            }
        }
    }

    private fun resolveDriveType(
        isPrimary: Boolean,
        isRemovable: Boolean,
        description: String
    ): StorageDriveType {
        if (isPrimary) return StorageDriveType.INTERNAL
        val descLower = description.lowercase(Locale.ROOT)
        return when {
            descLower.contains("usb") || descLower.contains("otg") -> StorageDriveType.USB
            isRemovable || descLower.contains("sd") -> StorageDriveType.SD_CARD
            else -> StorageDriveType.INTERNAL
        }
    }

    private fun extractVolumeRoot(fullPath: String, uuid: String): String {
        val index = fullPath.indexOf(uuid)
        return if (index != -1) {
            fullPath.substring(0, index + uuid.length)
        } else {
            fullPath.substringBefore("/Android/data")
        }
    }
}
