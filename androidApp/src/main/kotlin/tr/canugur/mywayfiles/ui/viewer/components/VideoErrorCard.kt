package tr.canugur.mywayfiles.ui.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Crash-proof error state card when video decoding or format playback fails.
 */
@Composable
fun VideoErrorCard(
    item: FileItem,
    errorMessage: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                text = errorMessage,
                color = Color(0xFFFF8A80),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.path,
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
