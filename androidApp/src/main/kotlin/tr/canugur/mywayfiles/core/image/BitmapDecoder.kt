package tr.canugur.mywayfiles.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DecodedBitmapResult(
    val imageBitmap: ImageBitmap,
    val originalWidth: Int,
    val originalHeight: Int
)

/**
 * Enterprise memory-safe Bitmap decoder with EXIF rotation support and downsampling.
 */
object BitmapDecoder {

    private const val DEFAULT_MAX_DIMENSION = 2560

    suspend fun decode(
        filePath: String,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Result<DecodedBitmapResult> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) {
                return@withContext Result.failure(IllegalArgumentException("File cannot be read: $filePath"))
            }

            // 1. Decode bounds only to determine dimensions without allocating bitmap memory
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, boundsOptions)

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                return@withContext Result.failure(IllegalStateException("Failed to decode image dimensions"))
            }

            // 2. Compute inSampleSize power-of-two downsampling
            var sampleSize = 1
            var halfWidth = rawWidth
            var halfHeight = rawHeight
            while (halfWidth > maxDimension || halfHeight > maxDimension) {
                sampleSize *= 2
                halfWidth /= 2
                halfHeight /= 2
            }

            // 3. Decode scaled Bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = BitmapFactory.decodeFile(filePath, decodeOptions)
                ?: return@withContext Result.failure(IllegalStateException("Failed to decode bitmap bytes"))

            // 4. Resolve EXIF rotation orientation
            val rotatedBitmap = applyExifRotation(filePath, rawBitmap)

            Result.success(
                DecodedBitmapResult(
                    imageBitmap = rotatedBitmap.asImageBitmap(),
                    originalWidth = rawWidth,
                    originalHeight = rawHeight
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun applyExifRotation(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationDegrees == 0f) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                rotated
            }
        } catch (_: Exception) {
            bitmap
        }
    }
}
