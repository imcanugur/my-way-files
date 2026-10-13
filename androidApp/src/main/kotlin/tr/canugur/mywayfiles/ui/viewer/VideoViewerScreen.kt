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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import tr.canugur.mywayfiles.core.util.FileShareHelper
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.explorer.components.PropertiesDialog
import tr.canugur.mywayfiles.ui.viewer.components.VideoErrorCard
import tr.canugur.mywayfiles.ui.viewer.components.VideoPlayerBottomBar
import tr.canugur.mywayfiles.ui.viewer.components.ViewerTopBar
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
            // Hardware Video Surface: Isolated per video path to eliminate MediaPlayer state corruption
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
                                        errorMessage = e.localizedMessage ?: "Oynatma başlatılamadı"
                                    }
                                }

                                // Auto-play next video upon completion posted asynchronously to avoid native deadlock
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
                                    errorMessage = e.localizedMessage ?: "Video yüklenirken hata oluştu"
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
                    onDismiss = onDismiss
                )
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

            // Bottom Player Control Panel
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                VideoPlayerBottomBar(
                    item = currentItem,
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
                    },
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
