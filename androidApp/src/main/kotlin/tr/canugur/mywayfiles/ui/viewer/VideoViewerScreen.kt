package tr.canugur.mywayfiles.ui.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.explorer.components.PropertiesDialog
import tr.canugur.mywayfiles.ui.theme.Dimensions
import java.io.File
import java.util.Locale

/**
 * Enterprise hardware-accelerated Video Player featuring bottom-docked media player controls,
 * clean video surface recreation via key(path) to eliminate native MediaPlayer freeze/deadlocks,
 * automatic playlist advancement, timeline scrubbing, 10s seek, and trash deletion.
 */
@Composable
fun VideoViewerScreen(
    videos: List<FileItem>,
    initialIndex: Int,
    onDelete: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onDismiss: () -> Unit,
    onIndexChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    var currentIndex by remember {
        mutableIntStateOf(initialIndex.coerceIn(0, videos.size - 1))
    }
    val currentItem = videos.getOrNull(currentIndex) ?: videos[0]

    LaunchedEffect(currentIndex) {
        onIndexChanged?.invoke(currentIndex)
    }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }
    var showProperties by remember { mutableStateOf(false) }

    // Auto-hide controls timer (3.5 seconds)
    LaunchedEffect(controlsVisible, isPlaying, isUserScrubbing) {
        if (controlsVisible && isPlaying && !isUserScrubbing) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Playback progress ticker coroutine
    LaunchedEffect(isPlaying, isUserScrubbing, currentItem.path) {
        while (isPlaying && !isUserScrubbing) {
            videoViewRef?.let { vv ->
                try {
                    currentPositionMs = vv.currentPosition
                    if (durationMs <= 0 && vv.duration > 0) {
                        durationMs = vv.duration
                    }
                } catch (_: Exception) {}
            }
            delay(250)
        }
    }

    BackHandler {
        onDismiss()
    }

    Surface(
        color = Color.Black,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Native Hardware Video Surface - Isolated per video using key(currentItem.path)
            // This prevents MediaPlayer / SurfaceHolder state corruption and deadlock
            key(currentItem.path) {
                var localVv by remember { mutableStateOf<VideoView?>(null) }

                DisposableEffect(currentItem.path) {
                    onDispose {
                        try {
                            localVv?.stopPlayback()
                            localVv = null
                        } catch (_: Exception) {}
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            localVv = this
                            videoViewRef = this

                            val file = File(currentItem.path)
                            if (!file.exists() || !file.canRead()) {
                                errorMessage = "Dosya bulunamadı veya okunamıyor: ${currentItem.path}"
                            } else {
                                errorMessage = null
                                currentPositionMs = 0
                                durationMs = 0

                                setOnPreparedListener { mp ->
                                    try {
                                        durationMs = mp.duration
                                        mp.start()
                                        isPlaying = true
                                        errorMessage = null
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage ?: e.message ?: "Oynatma başlatılamadı"
                                    }
                                }

                                // Auto-play next video upon completion posted asynchronously to avoid native callback deadlock
                                setOnCompletionListener {
                                    post {
                                        if (currentIndex < videos.size - 1) {
                                            currentIndex++
                                        } else {
                                            isPlaying = false
                                            currentPositionMs = durationMs
                                        }
                                    }
                                }

                                setOnErrorListener { _, what, extra ->
                                    errorMessage = "Video formatı veya codec desteklenmiyor (Hata: $what, Kod: $extra)"
                                    isPlaying = false
                                    true
                                }

                                try {
                                    setVideoPath(file.absolutePath)
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: e.message ?: "Video yüklenirken hata oluştu"
                                }
                            }
                        }
                    },
                    update = { vv ->
                        videoViewRef = vv
                        localVv = vv
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Playback Error State Card
            val activeError = errorMessage
            if (activeError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xEE121212))
                        .padding(Dimensions.SpacingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
                            .background(Color(0xFF1E1E1E))
                            .border(
                                width = Dimensions.CardBorderWidth,
                                color = Color(0xFFFF5252).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(Dimensions.CornerRadiusMedium)
                            )
                            .padding(Dimensions.SpacingLarge)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_video),
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(Dimensions.IconSizeLarge)
                        )
                        Text(
                            text = stringResource(R.string.video_load_failed),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeError,
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentItem.path,
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A2A2A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                        ) {
                            Text(text = stringResource(R.string.close_viewer), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Top Overlay Bar (Autohiding)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color(0xCC141414),
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
                            }

                            Text(
                                text = meta,
                                color = Color(0xFFB0B0B0),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Index Badge (e.g. 1 / 3)
                        if (videos.size > 1) {
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
                                    text = "${currentIndex + 1} / ${videos.size}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Player Control Panel (Timeline, Media Controls, and Action Toolbar)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color(0xEE141414),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimensions.SpacingMedium,
                                vertical = Dimensions.SpacingSmall
                            )
                    ) {
                        // 1. Timeline Scrubber & Timestamp
                        val displayPos = if (isUserScrubbing) scrubPositionMs.toInt() else currentPositionMs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(displayPos),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = if (durationMs > 0) {
                                    val current = if (isUserScrubbing) scrubPositionMs else currentPositionMs.toFloat()
                                    (current / durationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f,
                                onValueChange = { ratio ->
                                    isUserScrubbing = true
                                    scrubPositionMs = ratio * durationMs
                                },
                                onValueChangeFinished = {
                                    videoViewRef?.seekTo(scrubPositionMs.toInt())
                                    currentPositionMs = scrubPositionMs.toInt()
                                    isUserScrubbing = false
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4CC2FF),
                                    activeTrackColor = Color(0xFF4CC2FF),
                                    inactiveTrackColor = Color(0xFF404040)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = Dimensions.SpacingSmall)
                            )

                            Text(
                                text = formatDuration(durationMs),
                                color = Color(0xFFB0B0B0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 2. Playback Controls Bar (Previous, Rewind 10s, Play/Pause, Forward 10s, Next)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.SpacingExtraSmall),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Video
                            IconButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        videoViewRef?.stopPlayback()
                                        currentIndex--
                                    }
                                },
                                enabled = currentIndex > 0,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_skip_previous),
                                    contentDescription = "Previous Video",
                                    tint = if (currentIndex > 0) Color.White else Color(0x40FFFFFF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                            // 10s Rewind
                            IconButton(
                                onClick = {
                                    videoViewRef?.let { vv ->
                                        val target = (vv.currentPosition - 10000).coerceAtLeast(0)
                                        vv.seekTo(target)
                                        currentPositionMs = target
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_fast_rewind),
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                            // Prominent Play / Pause Button
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CC2FF))
                                    .clickable {
                                        videoViewRef?.let { vv ->
                                            if (vv.isPlaying) {
                                                vv.pause()
                                                isPlaying = false
                                            } else {
                                                vv.start()
                                                isPlaying = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                    ),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                            // 10s Fast Forward
                            IconButton(
                                onClick = {
                                    videoViewRef?.let { vv ->
                                        val target = (vv.currentPosition + 10000).coerceAtMost(durationMs)
                                        vv.seekTo(target)
                                        currentPositionMs = target
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_fast_forward),
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                            // Next Video
                            IconButton(
                                onClick = {
                                    if (currentIndex < videos.size - 1) {
                                        videoViewRef?.stopPlayback()
                                        currentIndex++
                                    }
                                },
                                enabled = currentIndex < videos.size - 1,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_skip_next),
                                    contentDescription = "Next Video",
                                    tint = if (currentIndex < videos.size - 1) Color.White else Color(0x40FFFFFF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Dimensions.SpacingExtraSmall))

                        // 3. Quick Actions Toolbar (Share, Copy, Properties, Delete)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VideoActionBottomItem(
                                iconRes = R.drawable.ic_share,
                                label = stringResource(R.string.action_share),
                                tint = Color.White,
                                onClick = { shareVideo(context, currentItem.path) }
                            )

                            VideoActionBottomItem(
                                iconRes = R.drawable.ic_copy,
                                label = stringResource(R.string.action_copy),
                                tint = Color.White,
                                onClick = { onCopy(currentItem) }
                            )

                            VideoActionBottomItem(
                                iconRes = R.drawable.ic_info,
                                label = stringResource(R.string.action_properties),
                                tint = Color.White,
                                onClick = { showProperties = true }
                            )

                            VideoActionBottomItem(
                                iconRes = R.drawable.ic_delete,
                                label = stringResource(R.string.action_delete),
                                tint = Color(0xFFFF6B6B),
                                onClick = {
                                    videoViewRef?.stopPlayback()
                                    onDelete(currentItem)
                                    if (videos.size <= 1) {
                                        onDismiss()
                                    } else if (currentIndex >= videos.size - 1) {
                                        currentIndex = (videos.size - 2).coerceAtLeast(0)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Properties Dialog
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
private fun VideoActionBottomItem(
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

private fun formatDuration(millis: Int): String {
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

private fun shareVideo(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))
        }
        context.startActivity(Intent.createChooser(fallbackIntent, null))
    }
}
