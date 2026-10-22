package tr.canugur.mywayfiles.ui.viewer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(fileItem.path) {
                            detectTapGestures(
                                onDoubleTap = {
                                    val newScale = if (scale > 1.05f) 1f else 2.5f
                                    scale = newScale
                                    offset = Offset.Zero
                                    onZoomScaleChanged(newScale)
                                },
                                onTap = { onToggleControls() }
                            )
                        }
                        .pointerInput(fileItem.path) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val count = event.changes.size

                                    if (count >= 2) {
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        scale = newScale
                                        onZoomScaleChanged(newScale)
                                        if (newScale > 1.05f) {
                                            offset += pan
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else if (scale > 1.05f && count == 1) {
                                        val pan = event.calculatePan()
                                        offset += pan
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
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
