package tr.canugur.mywayfiles.ui.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.DurationFormatter
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

enum class VideoPlaybackMode {
    PLAY_IN_ORDER,
    REPEAT_CURRENT,
    PLAY_ONCE
}

/** Focused playback controls, visually separate from the shared file action bar. */
@Composable
fun VideoPlaybackControls(
    playbackMode: VideoPlaybackMode,
    onPlaybackModeSelected: (VideoPlaybackMode) -> Unit,
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
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.mediaControlSurfaceStrong,
        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SpacingDefault)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DurationFormatter.format(if (isUserScrubbing) scrubPositionMs.toInt() else currentPositionMs),
                    color = AppColors.mediaTextPrimary,
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
                    enabled = durationMs > 0,
                    colors = SliderDefaults.colors(
                        thumbColor = AppColors.mediaAccent,
                        activeTrackColor = AppColors.mediaAccent,
                        inactiveTrackColor = AppColors.mediaProgressTrack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Dimensions.SpacingSmall)
                )
                Text(
                    text = DurationFormatter.format(durationMs),
                    color = AppColors.mediaTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                val modeLabel = stringResource(
                    when (playbackMode) {
                        VideoPlaybackMode.PLAY_IN_ORDER -> R.string.play_in_order
                        VideoPlaybackMode.REPEAT_CURRENT -> R.string.repeat_current_video
                        VideoPlaybackMode.PLAY_ONCE -> R.string.play_once
                    }
                )
                IconButton(
                    onClick = {
                        val nextMode = when (playbackMode) {
                            VideoPlaybackMode.PLAY_IN_ORDER -> VideoPlaybackMode.REPEAT_CURRENT
                            VideoPlaybackMode.REPEAT_CURRENT -> VideoPlaybackMode.PLAY_ONCE
                            VideoPlaybackMode.PLAY_ONCE -> VideoPlaybackMode.PLAY_IN_ORDER
                        }
                        onPlaybackModeSelected(nextMode)
                    },
                    modifier = Modifier.size(Dimensions.ActionButtonSize)
                ) {
                    Icon(
                        painter = painterResource(playbackModeIcon(playbackMode)),
                        contentDescription = modeLabel,
                        tint = if (playbackMode == VideoPlaybackMode.PLAY_ONCE) {
                            AppColors.mediaTextSecondary
                        } else {
                            AppColors.mediaAccent
                        },
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrevious, enabled = canSkipPrevious) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = stringResource(R.string.previous_video),
                        tint = if (canSkipPrevious) AppColors.mediaTextPrimary else AppColors.mediaTextDisabled,
                        modifier = Modifier.size(Dimensions.IconSizeDefault)
                    )
                }
                IconButton(onClick = onRewind10s) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fast_rewind),
                        contentDescription = stringResource(R.string.rewind_ten_seconds),
                        tint = AppColors.mediaTextPrimary,
                        modifier = Modifier.size(Dimensions.IconSizeDefault)
                    )
                }
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier
                        .size(Dimensions.ActionButtonSize)
                        .clip(CircleShape)
                        .background(AppColors.mediaAccent)
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = AppColors.mediaAccentContent,
                        modifier = Modifier.size(Dimensions.IconSizeDefault)
                    )
                }
                IconButton(onClick = onForward10s) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fast_forward),
                        contentDescription = stringResource(R.string.forward_ten_seconds),
                        tint = AppColors.mediaTextPrimary,
                        modifier = Modifier.size(Dimensions.IconSizeDefault)
                    )
                }
                IconButton(onClick = onSkipNext, enabled = canSkipNext) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = stringResource(R.string.next_video),
                        tint = if (canSkipNext) AppColors.mediaTextPrimary else AppColors.mediaTextDisabled,
                        modifier = Modifier.size(Dimensions.IconSizeDefault)
                    )
                }
            }
        }
    }
}

private fun playbackModeIcon(mode: VideoPlaybackMode): Int = when (mode) {
    VideoPlaybackMode.PLAY_IN_ORDER -> R.drawable.ic_skip_next
    VideoPlaybackMode.REPEAT_CURRENT -> R.drawable.ic_repeat
    VideoPlaybackMode.PLAY_ONCE -> R.drawable.ic_play
}
