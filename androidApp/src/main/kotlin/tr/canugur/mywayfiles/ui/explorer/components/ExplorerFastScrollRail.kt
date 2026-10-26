package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors

/** An overlay scrubber for large directories; it never changes the content or grid width. */
@Composable
fun ExplorerFastScrollRail(
    totalItemCount: Int,
    firstItemIndex: Int,
    visibleItemCount: Int,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalItemCount <= 0) return

    val maxFirstIndex = (totalItemCount - visibleItemCount).coerceAtLeast(1)
    val positionFraction = (firstItemIndex.toFloat() / maxFirstIndex).coerceIn(0f, 1f)
    val latestOnSeek by rememberUpdatedState(onSeek)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember(totalItemCount) { mutableFloatStateOf(positionFraction) }
    val shownFraction = if (isDragging) dragFraction else positionFraction
    val contextDescription = stringResource(R.string.fast_scroll_description)
    val positionDescription = stringResource(
        R.string.fast_scroll_position,
        (shownFraction * 100).roundToInt()
    )
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .semantics {
                contentDescription = contextDescription
                stateDescription = positionDescription
            },
        contentAlignment = Alignment.TopCenter
    ) {
        val railHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val thumbHeight = max(36.dp, (maxHeight * (visibleItemCount.toFloat() / totalItemCount))
            .coerceAtMost(64.dp))
        val travelPx = with(density) { (maxHeight - thumbHeight).coerceAtLeast(0.dp).toPx() }
        val thumbOffsetPx = (travelPx * shownFraction).roundToInt()
        val dragState = rememberDraggableState { delta ->
            dragFraction = (dragFraction + delta / railHeightPx).coerceIn(0f, 1f)
            latestOnSeek(dragFraction)
        }

        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(AppColors.divider)
        )
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(0, thumbOffsetPx) }
                .width(24.dp)
                .height(thumbHeight)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        isDragging = true
                        dragFraction = positionFraction
                    },
                    onDragStopped = { isDragging = false }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(if (isDragging) 6.dp else 4.dp)
                    .height(thumbHeight)
                    .clip(CircleShape)
                    .background(if (isDragging) AppColors.brandAccent else AppColors.textMuted)
            )
        }
    }
}
