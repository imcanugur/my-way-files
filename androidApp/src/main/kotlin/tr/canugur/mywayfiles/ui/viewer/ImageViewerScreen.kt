package tr.canugur.mywayfiles.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.BitmapDecoder
import tr.canugur.mywayfiles.core.image.DecodedBitmapResult
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.Dimensions

private sealed interface ImageViewerState {
    data object Loading : ImageViewerState
    data class Success(val decoded: DecodedBitmapResult) : ImageViewerState
    data class Error(val message: String) : ImageViewerState
}

/**
 * Enterprise image viewer featuring pinch-to-zoom, pan, double-tap zoom, and image metadata overlay.
 */
@Composable
fun ImageViewerScreen(
    fileItem: FileItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember(fileItem.path) { mutableStateOf<ImageViewerState>(ImageViewerState.Loading) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(fileItem.path) {
        state = ImageViewerState.Loading
        scale = 1f
        offset = Offset.Zero
        val result = BitmapDecoder.decode(fileItem.path)
        state = result.fold(
            onSuccess = { ImageViewerState.Success(it) },
            onFailure = { ImageViewerState.Error(it.localizedMessage ?: "Failed to decode image") }
        )
    }

    BackHandler {
        if (scale > 1f) {
            scale = 1f
            offset = Offset.Zero
        } else {
            onDismiss()
        }
    }

    Surface(
        color = Color(0xFF0F0F0F),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Canvas
            when (val s = state) {
                is ImageViewerState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF4CC2FF),
                            strokeWidth = Dimensions.SpacingMicro
                        )
                    }
                }

                is ImageViewerState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimensions.SpacingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.image_load_failed),
                            color = Color(0xFFFF99A4),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                        Text(
                            text = s.message,
                            color = Color(0xFF9E9E9E),
                            fontSize = 12.sp
                        )
                    }
                }

                is ImageViewerState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        offset += pan
                                    } else {
                                        offset = Offset.Zero
                                    }
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

            // Top Bar Overlay
            Surface(
                color = Color(0xCC1F1F1F),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Dimensions.SpacingSmall,
                            vertical = Dimensions.SpacingExtraSmall
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.close_viewer),
                            tint = Color.White,
                            modifier = Modifier.size(Dimensions.IconSizeMedium)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = Dimensions.SpacingSmall)
                    ) {
                        Text(
                            text = fileItem.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val stateVal = state
                        val resolution = if (stateVal is ImageViewerState.Success) {
                            stringResource(
                                R.string.image_dimensions,
                                stateVal.decoded.originalWidth,
                                stateVal.decoded.originalHeight
                            )
                        } else null

                        val meta = buildString {
                            if (resolution != null) append("$resolution • ")
                            append(ByteFormatter.format(fileItem.sizeBytes))
                            val dateStr = DateTimeFormatter.format(fileItem.lastModifiedMs)
                            if (dateStr.isNotEmpty()) append(" • $dateStr")
                        }

                        Text(
                            text = meta,
                            color = Color(0xFFB0B0B0),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
