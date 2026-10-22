package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** A single, quiet shortcut to restore deleted items. */
@Composable
fun RecycleBinCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Dimensions.SpacingMedium, horizontal = Dimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = AppColors.cardSurfaceSubtle, modifier = Modifier.size(Dimensions.IconContainerSize)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_recycle_bin),
                    contentDescription = null,
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeDefault)
                )
            }
        }
        Spacer(Modifier.width(Dimensions.SpacingDefault))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.recycle_bin),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.recycle_bin_description),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            tint = AppColors.textMuted,
            modifier = Modifier.size(Dimensions.IconSizeMedium).graphicsLayer(rotationZ = 180f)
        )
    }
}
