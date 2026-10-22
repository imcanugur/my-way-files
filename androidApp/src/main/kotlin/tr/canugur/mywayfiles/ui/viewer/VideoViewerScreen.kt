package tr.canugur.mywayfiles.ui.viewer

import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.FileShareHelper
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.explorer.components.PropertiesDialog
import tr.canugur.mywayfiles.ui.viewer.components.VideoErrorCard
import tr.canugur.mywayfiles.ui.viewer.components.VideoPlaybackControls
import tr.canugur.mywayfiles.ui.viewer.components.VideoPlaybackMode
import tr.canugur.mywayfiles.ui.viewer.components.ViewerBottomBar
import tr.canugur.mywayfiles.ui.viewer.components.ViewerTopBar
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import java.io.File

/**
 * Enterprise hardware-accelerated Video Player orchestrator with bottom docked controls,
 * clean key-isolated VideoView lifecycle, asynchronous playlist progression, and trash management.
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
    var playbackMode by remember { mutableStateOf(VideoPlaybackMode.PLAY_IN_ORDER) }
    val playbackModeState = rememberUpdatedState(playbackMode)
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var reloadToken by remember { mutableIntStateOf(0) }
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
        color = AppColors.mediaCanvas,
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
            // Hardware Video Surface: Isolated per video path to eliminate MediaPlayer state corruption
            key(currentItem.path, reloadToken) {
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
                        isBuffering = true

                            val file = File(currentItem.path)
                            if (!file.exists() || !file.canRead()) {
                                errorMessage = context.getString(R.string.video_file_unavailable)
                            } else {
                                errorMessage = null
                                currentPositionMs = 0
                                durationMs = 0

                                setOnPreparedListener { mp ->
                                    try {
                                        durationMs = mp.duration
                                        mp.start()
                                        isPlaying = true
                                        isBuffering = false
                                        errorMessage = null
                                    } catch (e: Exception) {
                                        isBuffering = false
                                        errorMessage = e.localizedMessage ?: context.getString(R.string.video_playback_failed)
                                    }
                                }

                                // Auto-play next video upon completion posted asynchronously to avoid native deadlock
                                setOnCompletionListener {
                                    post {
                                        when (playbackModeState.value) {
                                            VideoPlaybackMode.REPEAT_CURRENT -> {
                                                seekTo(0)
                                                start()
                                                currentPositionMs = 0
                                                isPlaying = true
                                            }
                                            VideoPlaybackMode.PLAY_IN_ORDER -> {
                                                if (currentIndex < videos.size - 1) {
                                                    currentIndex++
                                                } else {
                                                    isPlaying = false
                                                    currentPositionMs = durationMs
                                                }
                                            }
                                            VideoPlaybackMode.PLAY_ONCE -> {
                                                isPlaying = false
                                                currentPositionMs = durationMs
                                            }
                                        }
                                    }
                                }

                                setOnErrorListener { _, what, extra ->
                                    errorMessage = context.getString(R.string.video_unsupported, what, extra)
                                    isPlaying = false
                                    isBuffering = false
                                    true
                                }

                                try {
                                    setVideoPath(file.absolutePath)
                                } catch (e: Exception) {
                                    isBuffering = false
                                    errorMessage = e.localizedMessage ?: context.getString(R.string.video_load_failed)
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
                VideoErrorCard(
                    item = currentItem,
                    errorMessage = activeError,
                    onRetry = {
                        errorMessage = null
                        isPlaying = false
                        isBuffering = true
                        reloadToken++
                    },
                    onDismiss = onDismiss
                )
            }

            if (isBuffering && errorMessage == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = AppColors.mediaAccent)
                    androidx.compose.material3.Text(
                        text = currentItem.name,
                        color = AppColors.mediaTextPrimary,
                        maxLines = 1
                    )
                }
            }

            // Top Overlay Bar (Autohiding)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ViewerTopBar(
                    item = currentItem,
                    currentIndex = currentIndex,
                    totalCount = videos.size,
                    onDismiss = onDismiss
                )
            }

            // Keep playback controls separate from the same file-action bar used by the image viewer.
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    VideoPlaybackControls(
                        playbackMode = playbackMode,
                        onPlaybackModeSelected = { playbackMode = it },
                        isPlaying = isPlaying,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        isUserScrubbing = isUserScrubbing,
                        scrubPositionMs = scrubPositionMs,
                        canSkipPrevious = currentIndex > 0,
                        canSkipNext = currentIndex < videos.size - 1,
                        onScrubChange = { ratio ->
                            isUserScrubbing = true
                            scrubPositionMs = ratio * durationMs
                        },
                        onScrubFinished = {
                            videoViewRef?.seekTo(scrubPositionMs.toInt())
                            currentPositionMs = scrubPositionMs.toInt()
                            isUserScrubbing = false
                        },
                        onPlayPauseToggle = {
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
                        onRewind10s = {
                            videoViewRef?.let { vv ->
                                val target = (vv.currentPosition - 10000).coerceAtLeast(0)
                                vv.seekTo(target)
                                currentPositionMs = target
                            }
                        },
                        onForward10s = {
                            videoViewRef?.let { vv ->
                                val target = (vv.currentPosition + 10000).coerceAtMost(durationMs)
                                vv.seekTo(target)
                                currentPositionMs = target
                            }
                        },
                        onSkipPrevious = {
                            if (currentIndex > 0) {
                                videoViewRef?.stopPlayback()
                                currentIndex--
                            }
                        },
                        onSkipNext = {
                            if (currentIndex < videos.size - 1) {
                                videoViewRef?.stopPlayback()
                                currentIndex++
                            }
                        }
                    )
                    ViewerBottomBar(
                        item = currentItem,
                        onShare = { FileShareHelper.shareFile(context, it.path) },
                        onCopy = onCopy,
                        onProperties = { showProperties = true },
                        onDelete = {
                            videoViewRef?.stopPlayback()
                            onDelete(it)
                            if (videos.size <= 1) {
                                onDismiss()
                            } else if (currentIndex >= videos.size - 1) {
                                currentIndex = (videos.size - 2).coerceAtLeast(0)
                            }
                        }
                    )
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
