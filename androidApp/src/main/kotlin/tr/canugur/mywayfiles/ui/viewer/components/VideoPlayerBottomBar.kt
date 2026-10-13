package tr.canugur.mywayfiles.ui.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.DurationFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Bottom-docked media player control panel containing scrubber timeline, playback controls,
 * and quick-action toolbar.
 */
@Composable
fun VideoPlayerBottomBar(
    item: FileItem,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    isUserScrubbing: Boolean,
    scrubPositionMs: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onScrubChange: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onRewind10s: () -> Unit,
    onForward10s: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onShare: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xEE141414),
        modifier = modifier
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
                    text = DurationFormatter.format(displayPos),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = if (durationMs > 0) {
                        val current = if (isUserScrubbing) scrubPositionMs else currentPositionMs.toFloat()
                        (current / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f,
                    onValueChange = onScrubChange,
                    onValueChangeFinished = onScrubFinished,
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
                    text = DurationFormatter.format(durationMs),
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
                    onClick = onSkipPrevious,
                    enabled = canSkipPrevious,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "Previous Video",
                        tint = if (canSkipPrevious) Color.White else Color(0x40FFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                // 10s Rewind
                IconButton(
                    onClick = onRewind10s,
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
                        .clickable { onPlayPauseToggle() },
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
                    onClick = onForward10s,
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
                    onClick = onSkipNext,
                    enabled = canSkipNext,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "Next Video",
                        tint = if (canSkipNext) Color.White else Color(0x40FFFFFF),
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
                ViewerActionItem(
                    iconRes = R.drawable.ic_share,
                    label = stringResource(R.string.action_share),
                    tint = Color.White,
                    onClick = { onShare(item) }
                )

                ViewerActionItem(
                    iconRes = R.drawable.ic_copy,
                    label = stringResource(R.string.action_copy),
                    tint = Color.White,
                    onClick = { onCopy(item) }
                )

                ViewerActionItem(
                    iconRes = R.drawable.ic_info,
                    label = stringResource(R.string.action_properties),
                    tint = Color.White,
                    onClick = { onProperties(item) }
                )

                ViewerActionItem(
                    iconRes = R.drawable.ic_delete,
                    label = stringResource(R.string.action_delete),
                    tint = Color(0xFFFF6B6B),
                    onClick = { onDelete(item) }
                )
            }
        }
    }
}
