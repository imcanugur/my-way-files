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
import kotlinx.coroutines.launch
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.BitmapDecoder
import tr.canugur.mywayfiles.core.image.DecodedBitmapResult
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.explorer.components.DeleteConfirmDialog
import tr.canugur.mywayfiles.ui.explorer.components.PropertiesDialog
import tr.canugur.mywayfiles.ui.theme.Dimensions
import java.io.File

private sealed interface ImagePageState {
    data object Loading : ImagePageState
    data class Success(val decoded: DecodedBitmapResult) : ImagePageState
    data class Error(val message: String) : ImagePageState
}

/**
 * Enterprise gallery-grade image viewer with horizontal swipe pager, pinch-to-zoom,
 * pan, double-tap zoom, photo counter, and quick action toolbar (delete, share, copy, info).
 */
@Composable
fun ImageViewerScreen(
    images: List<FileItem>,
    initialIndex: Int,
    onDelete: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onDismiss: () -> Unit,
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showProperties by remember { mutableStateOf(false) }

    val currentItem = images.getOrNull(pagerState.currentPage) ?: images[0]

    BackHandler {
        if (isCurrentZoomed) {
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
            // Horizontal Pager for swiping between images
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = images[page]
                InteractiveImagePage(
                    fileItem = item,
                    isCurrentPage = page == pagerState.currentPage,
                    onZoomScaleChanged = { scale ->
                        if (page == pagerState.currentPage) {
                            isCurrentZoomed = scale > 1.05f
                        }
                    }
                )
            }

            // Navigation Arrow: Previous Image (when not zoomed)
            if (!isCurrentZoomed && pagerState.currentPage > 0) {
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

            // Navigation Arrow: Next Image (when not zoomed)
            if (!isCurrentZoomed && pagerState.currentPage < images.size - 1) {
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

            // Top Bar Overlay
            Surface(
                color = Color(0xDD181818),
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

            // Bottom Quick Action Bar (when not zoomed)
            AnimatedVisibility(
                visible = !isCurrentZoomed,
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
                        // Share Action
                        ActionBottomItem(
                            iconRes = R.drawable.ic_share,
                            label = stringResource(R.string.action_share),
                            tint = Color.White,
                            onClick = { shareImage(context, currentItem.path) }
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

                        // Delete Action
                        ActionBottomItem(
                            iconRes = R.drawable.ic_delete,
                            label = stringResource(R.string.action_delete),
                            tint = Color(0xFFFF6B6B),
                            onClick = { showDeleteConfirm = true }
                        )
                    }
                }
            }

            // Dialogs
            if (showDeleteConfirm) {
                DeleteConfirmDialog(
                    fileName = currentItem.name,
                    onConfirm = {
                        onDelete(currentItem)
                        showDeleteConfirm = false
                    },
                    onDismiss = { showDeleteConfirm = false }
                )
            }

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

private fun shareImage(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))
        }
        context.startActivity(Intent.createChooser(fallbackIntent, null))
    }
}

@Composable
private fun InteractiveImagePage(
    fileItem: FileItem,
    isCurrentPage: Boolean,
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
