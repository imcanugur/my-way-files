package tr.canugur.mywayfiles.ui.viewer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.BitmapDecoder
import tr.canugur.mywayfiles.core.image.DecodedBitmapResult
import tr.canugur.mywayfiles.core.image.ThumbnailLoader
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DurationFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.AppColors

private sealed interface VideoPreviewState {
    data object Loading : VideoPreviewState
    data class Success(val decoded: DecodedBitmapResult) : VideoPreviewState
    data class Error(val message: String) : VideoPreviewState
}

/**
 * High-definition video preview slide featuring extracted poster frame, metadata chip, and centered play button.
 */
@Composable
fun VideoSlidePage(
    fileItem: FileItem,
    isCurrentPage: Boolean,
    onPlay: () -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember(fileItem.path) { mutableStateOf<VideoPreviewState>(VideoPreviewState.Loading) }
    var durationMs by remember(fileItem.path) { mutableLongStateOf(0L) }

    LaunchedEffect(fileItem.path) {
        state = VideoPreviewState.Loading
        durationMs = BitmapDecoder.getVideoDurationMs(fileItem.path)
        val result = BitmapDecoder.decodeVideoFrame(fileItem.path)
        state = result.fold(
            onSuccess = { VideoPreviewState.Success(it) },
            onFailure = {
                val cached = ThumbnailLoader.getFromCache(fileItem.path)
                if (cached != null) {
                    VideoPreviewState.Success(DecodedBitmapResult(cached, cached.width, cached.height))
                } else {
                    VideoPreviewState.Error(it.localizedMessage ?: "Failed to load video preview")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(fileItem.path) {
                detectTapGestures(
                    onTap = { onToggleControls() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is VideoPreviewState.Loading -> {
                CircularProgressIndicator(
                    color = AppColors.mediaAccent,
                    strokeWidth = Dimensions.SpacingMicro
                )
            }

            is VideoPreviewState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.mediaSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                    ) {
                        PlayButtonCircle(onClick = onPlay)
                        Text(
                            text = fileItem.name,
                            color = AppColors.mediaTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            is VideoPreviewState.Success -> {
                Image(
                    bitmap = s.decoded.imageBitmap,
                    contentDescription = fileItem.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // High-contrast translucent vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.mediaCanvas.copy(alpha = 0.35f))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                ) {
                    PlayButtonCircle(onClick = onPlay)

                    val durationBadge = if (durationMs > 0) {
                        DurationFormatter.format(durationMs) + " • " + ByteFormatter.format(fileItem.sizeBytes)
                    } else {
                        stringResource(R.string.media_type_video).uppercase() + " • " + ByteFormatter.format(fileItem.sizeBytes)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimensions.CornerRadiusPill))
                            .background(AppColors.mediaControlSurfaceStrong)
                            .border(
                                width = 1.dp,
                                color = AppColors.mediaControlBorder,
                                shape = RoundedCornerShape(Dimensions.CornerRadiusPill)
                            )
                            .padding(
                                horizontal = Dimensions.SpacingMedium,
                                vertical = Dimensions.SpacingExtraSmall
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingExtraSmall)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_video),
                                contentDescription = null,
                                tint = AppColors.mediaAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = durationBadge,
                                color = AppColors.mediaTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayButtonCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(AppColors.mediaControlSurface)
            .border(
                width = 2.dp,
                color = AppColors.mediaAccent.copy(alpha = 0.85f),
                shape = CircleShape
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_play),
            contentDescription = stringResource(R.string.play),
            tint = AppColors.mediaTextPrimary,
            modifier = Modifier
                .size(36.dp)
                .offset(x = 2.dp)
        )
    }
}
