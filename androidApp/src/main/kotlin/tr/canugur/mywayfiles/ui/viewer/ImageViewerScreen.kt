package tr.canugur.mywayfiles.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.constants.MediaViewerTiming
import tr.canugur.mywayfiles.core.util.FileShareHelper
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.explorer.components.PropertiesDialog
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.viewer.components.InteractiveImagePage
import tr.canugur.mywayfiles.ui.viewer.components.VideoSlidePage
import tr.canugur.mywayfiles.ui.viewer.components.ViewerBottomBar
import tr.canugur.mywayfiles.ui.viewer.components.ViewerTopBar

/**
 * Enterprise gallery orchestrator uniting photos and videos in a single swipeable pager.
 * Manages immersion timers, pinch zoom constraints, and video player delegation.
 */
@Composable
fun ImageViewerScreen(
    images: List<FileItem>,
    initialIndex: Int,
    onDelete: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onMoveToFolder: (FileItem) -> Unit,
    onDismiss: () -> Unit,
    autoHideControls: Boolean = true,
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
    LaunchedEffect(autoHideControls, controlsVisible, pagerState.currentPage, isCurrentZoomed) {
        if (autoHideControls && controlsVisible && !isCurrentZoomed && playingVideoItem == null) {
            delay(MediaViewerTiming.CONTROL_AUTO_HIDE_DELAY_MS)
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
        color = AppColors.mediaCanvas,
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

            // Navigation Arrow: Previous Media
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
                        .size(Dimensions.ActionButtonSize)
                        .clip(CircleShape)
                        .background(AppColors.mediaControlSurface)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.previous_item),
                        tint = AppColors.mediaTextPrimary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }
            }

            // Navigation Arrow: Next Media
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
                        .size(Dimensions.ActionButtonSize)
                        .clip(CircleShape)
                        .background(AppColors.mediaControlSurface)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.next_item),
                        tint = AppColors.mediaTextPrimary,
                        modifier = Modifier
                            .size(Dimensions.IconSizeMedium)
                            .graphicsLayer(rotationZ = 180f)
                    )
                }
            }

            // Top Bar Overlay
            AnimatedVisibility(
                visible = controlsVisible && !isCurrentZoomed && playingVideoItem == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ViewerTopBar(
                    item = currentItem,
                    currentIndex = pagerState.currentPage,
                    totalCount = images.size,
                    onDismiss = onDismiss
                )
            }

            // Bottom Quick Action Bar
            AnimatedVisibility(
                visible = controlsVisible && !isCurrentZoomed && playingVideoItem == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ViewerBottomBar(
                    item = currentItem,
                    onShare = { FileShareHelper.shareFile(context, it.path) },
                    onCopy = onCopy,
                    onMoveToFolder = onMoveToFolder,
                    onProperties = { showProperties = true },
                    onDelete = onDelete
                )
            }

            // Hardware Video Player Overlay
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
                    onMoveToFolder = onMoveToFolder,
                    autoHideControls = autoHideControls,
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
