package tr.canugur.mywayfiles.ui.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.BitmapDecoder
import tr.canugur.mywayfiles.core.image.DecodedBitmapResult
import tr.canugur.mywayfiles.core.image.ThumbnailLoader
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.explorer.components.PropertiesDialog
import tr.canugur.mywayfiles.ui.theme.Dimensions
import java.io.File
import java.util.Locale

private sealed interface ImagePageState {
    data object Loading : ImagePageState
    data class Success(val decoded: DecodedBitmapResult) : ImagePageState
    data class Error(val message: String) : ImagePageState
}

/**
 * Enterprise gallery-grade unified Media Viewer (Images & Videos in the same swipe queue).
 * Supports immersive autohiding top and bottom bars (with 3.5s inactivity timer and tap toggle),
 * pinch-to-zoom for photos, high-res video preview slides with one-touch launch play button,
 * swipe navigation, and quick action toolbar (share, copy, properties, instant trash deletion).
 */
@Composable
fun ImageViewerScreen(
    images: List<FileItem>,
    initialIndex: Int,
    onDelete: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onDismiss: () -> Unit,
    initialPlayVideo: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    val safeInitial = initialIndex.coerceIn(0, images.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitial) { images.size }
    val scope = rememberCoroutineScope()

    var isCurrentZoomed by remember { mutableStateOf(false) }
    var showProperties by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    var playingVideoItem by remember(initialIndex, initialPlayVideo) {
        mutableStateOf<FileItem?>(
            if (initialPlayVideo && images.isNotEmpty() && FileTypeClassifier.isVideo(images[safeInitial])) {
                images[safeInitial]
            } else null
        )
    }

    // Auto-hide top and bottom bars after 3.5 seconds of inactivity
    LaunchedEffect(controlsVisible, pagerState.currentPage, isCurrentZoomed) {
        if (controlsVisible && !isCurrentZoomed && playingVideoItem == null) {
            delay(3500)
            controlsVisible = false
        }
    }

    val currentItem = images.getOrNull(pagerState.currentPage) ?: images[0]

    BackHandler {
        if (playingVideoItem != null) {
            playingVideoItem = null
        } else if (isCurrentZoomed) {
            isCurrentZoomed = false
        } else {
            onDismiss()
        }
    }

    Surface(
        color = Color(0xFF0A0A0A),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Horizontal Pager for swiping between both photos and videos
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = images[page]
                if (FileTypeClassifier.isVideo(item)) {
                    VideoSlidePage(
                        fileItem = item,
                        isCurrentPage = page == pagerState.currentPage,
                        onPlay = { playingVideoItem = item },
                        onToggleControls = { controlsVisible = !controlsVisible }
                    )
                } else {
                    InteractiveImagePage(
                        fileItem = item,
                        isCurrentPage = page == pagerState.currentPage,
                        onToggleControls = { controlsVisible = !controlsVisible },
                        onZoomScaleChanged = { scale ->
                            if (page == pagerState.currentPage) {
                                isCurrentZoomed = scale > 1.05f
                            }
                        }
                    )
                }
            }

            // Navigation Arrow: Previous Media (when not zoomed and controls visible)
            if (controlsVisible && !isCurrentZoomed && playingVideoItem == null && pagerState.currentPage > 0) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = Dimensions.SpacingMedium)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x99202020))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }
            }

            // Navigation Arrow: Next Media (when not zoomed and controls visible)
            if (controlsVisible && !isCurrentZoomed && playingVideoItem == null && pagerState.currentPage < images.size - 1) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = Dimensions.SpacingMedium)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x99202020))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier
                            .size(Dimensions.IconSizeMedium)
                            .graphicsLayer(rotationZ = 180f)
                    )
                }
            }

            // Top Bar Overlay (Autohiding with fade animation)
            AnimatedVisibility(
                visible = controlsVisible && !isCurrentZoomed && playingVideoItem == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color(0xDD181818),
                    modifier = Modifier
                        .fillMaxWidth()
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
                                .padding(horizontal = Dimensions.SpacingSmall)
                        ) {
                            Text(
                                text = currentItem.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val meta = buildString {
                                append(ByteFormatter.format(currentItem.sizeBytes))
                                val dateStr = DateTimeFormatter.format(currentItem.lastModifiedMs)
                                if (dateStr.isNotEmpty()) append(" • $dateStr")
                                if (FileTypeClassifier.isVideo(currentItem)) {
                                    append(" • Video")
                                }
                            }

                            Text(
                                text = meta,
                                color = Color(0xFFB0B0B0),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Gallery Index Pill Badge (e.g. 3 / 24)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimensions.CornerRadiusPill))
                                .background(Color(0xFF2A2A2A))
                                .padding(
                                    horizontal = Dimensions.SpacingMedium,
                                    vertical = Dimensions.SpacingExtraSmall
                                )
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${images.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Bottom Quick Action Bar (Autohiding with fade animation)
            AnimatedVisibility(
                visible = controlsVisible && !isCurrentZoomed && playingVideoItem == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color(0xEE181818),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = Dimensions.SpacingSmall,
                                horizontal = Dimensions.SpacingMedium
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share Action (Images or Videos)
                        ActionBottomItem(
                            iconRes = R.drawable.ic_share,
                            label = stringResource(R.string.action_share),
                            tint = Color.White,
                            onClick = { shareMedia(context, currentItem.path) }
                        )

                        // Copy Action
                        ActionBottomItem(
                            iconRes = R.drawable.ic_copy,
                            label = stringResource(R.string.action_copy),
                            tint = Color.White,
                            onClick = { onCopy(currentItem) }
                        )

                        // Properties Action
                        ActionBottomItem(
                            iconRes = R.drawable.ic_info,
                            label = stringResource(R.string.action_properties),
                            tint = Color.White,
                            onClick = { showProperties = true }
                        )

                        // Delete Action (moves directly to Recycle Bin)
                        ActionBottomItem(
                            iconRes = R.drawable.ic_delete,
                            label = stringResource(R.string.action_delete),
                            tint = Color(0xFFFF6B6B),
                            onClick = { onDelete(currentItem) }
                        )
                    }
                }
            }

            // High-Performance Hardware Video Player Overlay
            val activeVideo = playingVideoItem
            if (activeVideo != null) {
                val allVideos = remember(images) { images.filter { FileTypeClassifier.isVideo(it) } }
                val videoIndex = allVideos.indexOfFirst { it.path == activeVideo.path }.coerceAtLeast(0)
                VideoViewerScreen(
                    videos = allVideos.ifEmpty { listOf(activeVideo) },
                    initialIndex = videoIndex,
                    onDelete = { deleted ->
                        playingVideoItem = null
                        onDelete(deleted)
                    },
                    onCopy = onCopy,
                    onIndexChanged = { newIdx ->
                        val currentPlayed = allVideos.getOrNull(newIdx)
                        if (currentPlayed != null) {
                            val mediaIdx = images.indexOfFirst { it.path == currentPlayed.path }
                            if (mediaIdx >= 0 && mediaIdx != pagerState.currentPage) {
                                scope.launch { pagerState.scrollToPage(mediaIdx) }
                            }
                        }
                    },
                    onDismiss = { playingVideoItem = null }
                )
            }

            // Dialogs
            if (showProperties) {
                PropertiesDialog(
                    item = currentItem,
                    onDismiss = { showProperties = false }
                )
            }
        }
    }
}

@Composable
private fun VideoSlidePage(
    fileItem: FileItem,
    isCurrentPage: Boolean,
    onPlay: () -> Unit,
    onToggleControls: () -> Unit
) {
    var state by remember(fileItem.path) { mutableStateOf<ImagePageState>(ImagePageState.Loading) }
    var durationMs by remember(fileItem.path) { mutableLongStateOf(0L) }

    LaunchedEffect(fileItem.path) {
        state = ImagePageState.Loading
        durationMs = BitmapDecoder.getVideoDurationMs(fileItem.path)
        val result = BitmapDecoder.decodeVideoFrame(fileItem.path)
        state = result.fold(
            onSuccess = { ImagePageState.Success(it) },
            onFailure = {
                val cached = ThumbnailLoader.getFromCache(fileItem.path)
                if (cached != null) {
                    ImagePageState.Success(DecodedBitmapResult(cached, cached.width, cached.height))
                } else {
                    ImagePageState.Error(it.localizedMessage ?: "Failed to load video preview")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(fileItem.path) {
                detectTapGestures(
                    onTap = { onToggleControls() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is ImagePageState.Loading -> {
                CircularProgressIndicator(
                    color = Color(0xFF4CC2FF),
                    strokeWidth = Dimensions.SpacingMicro
                )
            }

            is ImagePageState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF141414)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                    ) {
                        PlayButtonCircle(onClick = onPlay)
                        Text(
                            text = fileItem.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            is ImagePageState.Success -> {
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
                        .background(Color.Black.copy(alpha = 0.35f))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                ) {
                    PlayButtonCircle(onClick = onPlay)

                    val durationBadge = if (durationMs > 0) {
                        formatDurationBadge(durationMs) + " • " + ByteFormatter.format(fileItem.sizeBytes)
                    } else {
                        "VIDEO • " + ByteFormatter.format(fileItem.sizeBytes)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimensions.CornerRadiusPill))
                            .background(Color(0xCC181818))
                            .border(
                                width = 1.dp,
                                color = Color(0x33FFFFFF),
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
                                tint = Color(0xFF4CC2FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = durationBadge,
                                color = Color.White,
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
private fun PlayButtonCircle(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0xD91E1E1E))
            .border(
                width = 2.dp,
                color = Color(0xFF4CC2FF).copy(alpha = 0.85f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_play),
            contentDescription = "Play Video",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .offset(x = 2.dp)
        )
    }
}

private fun formatDurationBadge(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun ActionBottomItem(
    iconRes: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
            .clickable { onClick() }
            .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingExtraSmall)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(Dimensions.IconSizeMedium)
        )
        Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun shareMedia(context: Context, filePath: String) {
    val isVideo = FileTypeClassifier.isVideo(filePath)
    val mimeType = if (isVideo) "video/*" else "image/*"
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))
        }
        context.startActivity(Intent.createChooser(fallbackIntent, null))
    }
}

@Composable
private fun InteractiveImagePage(
    fileItem: FileItem,
    isCurrentPage: Boolean,
    onToggleControls: () -> Unit,
    onZoomScaleChanged: (Float) -> Unit
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is ImagePageState.Loading -> {
                CircularProgressIndicator(
                    color = Color(0xFF4CC2FF),
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
                        color = Color(0xFFFF99A4),
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
                                onTap = {
                                    onToggleControls()
                                }
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
