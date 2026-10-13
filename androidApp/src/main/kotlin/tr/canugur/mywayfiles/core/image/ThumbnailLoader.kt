package tr.canugur.mywayfiles.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import java.io.File

/**
 * Enterprise thread-safe LRU-cached thumbnail loader for high-performance LazyColumn rendering.
 */
object ThumbnailLoader {

    private const val THUMBNAIL_MAX_DIMENSION = 192

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheCapacityKb = (maxMemoryKb / 8).coerceIn(4096, 32768)

    private val memoryCache = object : LruCache<String, ImageBitmap>(cacheCapacityKb) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return (value.width * value.height * 2) / 1024
        }
    }

    fun getFromCache(path: String): ImageBitmap? {
        return memoryCache.get(path)
    }

    suspend fun loadThumbnail(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(path)?.let { return@withContext it }

        try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return@withContext null

            val ext = file.extension.lowercase()
            if (FileTypeClassifier.isVideo(ext)) {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(path)
                    val frame = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (frame != null) {
                        val maxDim = THUMBNAIL_MAX_DIMENSION
                        val w = frame.width
                        val h = frame.height
                        val scaled = if (w > maxDim || h > maxDim) {
                            val ratio = w.toFloat() / h.toFloat()
                            val (targetW, targetH) = if (ratio > 1) {
                                maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
                            } else {
                                (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
                            }
                            Bitmap.createScaledBitmap(frame, targetW, targetH, true)
                        } else {
                            frame
                        }
                        val imageBitmap = scaled.asImageBitmap()
                        memoryCache.put(path, imageBitmap)
                        return@withContext imageBitmap
                    }
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, boundsOptions)

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return@withContext null

            var sampleSize = 1
            var halfWidth = rawWidth
            var halfHeight = rawHeight
            while (halfWidth > THUMBNAIL_MAX_DIMENSION || halfHeight > THUMBNAIL_MAX_DIMENSION) {
                sampleSize *= 2
                halfWidth /= 2
                halfHeight /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val rawBitmap = BitmapFactory.decodeFile(path, decodeOptions) ?: return@withContext null
            val rotatedBitmap = applyExifRotation(path, rawBitmap)
            val imageBitmap = rotatedBitmap.asImageBitmap()

            memoryCache.put(path, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun applyExifRotation(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) bitmap.recycle()
                rotated
            }
        } catch (_: Exception) {
            bitmap
        }
    }
}
