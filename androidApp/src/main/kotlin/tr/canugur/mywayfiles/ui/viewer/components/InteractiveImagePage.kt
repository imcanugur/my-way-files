package tr.canugur.mywayfiles.ui.viewer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.BitmapDecoder
import tr.canugur.mywayfiles.core.image.DecodedBitmapResult
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.AppColors

private sealed interface ImagePageState {
    data object Loading : ImagePageState
    data class Success(val decoded: DecodedBitmapResult) : ImagePageState
    data class Error(val message: String) : ImagePageState
}

/**
 * Interactive image view page supporting smooth pinch-to-zoom, panning, and double-tap zoom.
 */
@Composable
fun InteractiveImagePage(
    fileItem: FileItem,
    isCurrentPage: Boolean,
    onToggleControls: () -> Unit,
    onZoomScaleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember(fileItem.path) { mutableStateOf<ImagePageState>(ImagePageState.Loading) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)
    val currentOnZoomScaleChanged by rememberUpdatedState(onZoomScaleChanged)

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
            onZoomScaleChanged(1f)
        }
    }

    LaunchedEffect(fileItem.path) {
        state = ImagePageState.Loading
        scale = 1f
        offset = Offset.Zero
        val result = BitmapDecoder.decode(fileItem.path)
        state = result.fold(
            onSuccess = { ImagePageState.Success(it) },
            onFailure = { ImagePageState.Error(it.localizedMessage ?: "Failed to decode image") }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is ImagePageState.Loading -> {
                CircularProgressIndicator(
                    color = AppColors.mediaAccent,
                    strokeWidth = Dimensions.SpacingMicro
                )
            }

            is ImagePageState.Error -> {
                Column(
                    modifier = Modifier.padding(Dimensions.SpacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.image_load_failed),
                        color = AppColors.mediaErrorMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            is ImagePageState.Success -> {
                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                    scale = newScale
                    offset = if (newScale <= 1.05f) {
                        Offset.Zero
                    } else {
                        clampImageOffset(
                            offset + panChange,
                            newScale,
                            viewportSize.width.toFloat(),
                            viewportSize.height.toFloat(),
                            s.decoded.imageBitmap.width.toFloat(),
                            s.decoded.imageBitmap.height.toFloat()
                        )
                    }
                    currentOnZoomScaleChanged(newScale)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewportSize = it }
                        .pointerInput(fileItem.path) {
                            detectTapGestures(
                                onDoubleTap = { tap ->
                                    val newScale = if (currentScale > 1.05f) 1f else 2.5f
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val nextOffset = if (newScale == 1f) {
                                        Offset.Zero
                                    } else {
                                        // Keep the tapped image point under the finger while zooming.
                                        (tap - center) - ((tap - center) - currentOffset) * (newScale / currentScale)
                                    }
                                    scale = newScale
                                    offset = clampImageOffset(
                                        nextOffset,
                                        newScale,
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                        s.decoded.imageBitmap.width.toFloat(),
                                        s.decoded.imageBitmap.height.toFloat()
                                    )
                                    currentOnZoomScaleChanged(newScale)
                                },
                                onTap = { onToggleControls() }
                            )
                        }
                        .transformable(
                            state = transformState,
                            canPan = { scale > 1.05f }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = s.decoded.imageBitmap,
                        contentDescription = fileItem.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }
        }
    }
}

private fun clampImageOffset(
    offset: Offset,
    scale: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): Offset {
    // ContentScale.Fit may leave space around the image. Pan limits must use the
    // rendered image bounds, otherwise portrait/landscape images drift into empty space.
    val imageAspectRatio = imageWidth / imageHeight
    val viewportAspectRatio = viewportWidth / viewportHeight
    val fittedWidth: Float
    val fittedHeight: Float
    if (imageAspectRatio > viewportAspectRatio) {
        fittedWidth = viewportWidth
        fittedHeight = viewportWidth / imageAspectRatio
    } else {
        fittedHeight = viewportHeight
        fittedWidth = viewportHeight * imageAspectRatio
    }
    val maxX = fittedWidth * (scale - 1f) / 2f
    val maxY = fittedHeight * (scale - 1f) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}
