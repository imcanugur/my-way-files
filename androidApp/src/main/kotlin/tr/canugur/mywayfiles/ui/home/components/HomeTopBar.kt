package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Material top app bar for the storage overview. */
@Composable
fun HomeTopBar(
    isDarkTheme: Boolean,
    isRefreshing: Boolean,
    onToggleTheme: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.canvasBackground,
        modifier = modifier.fillMaxWidth().statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SpacingLarge, vertical = Dimensions.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing,
                modifier = Modifier.size(Dimensions.ActionButtonSize)
            ) {
                if (isRefreshing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.IconSizeMedium),
                        strokeWidth = Dimensions.SpacingMicro,
                        color = AppColors.brandAccent
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.refresh_drives),
                        tint = AppColors.textSecondary
                    )
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.size(Dimensions.ActionButtonSize)
            ) {
                Icon(
                    painter = painterResource(
                        if (isDarkTheme) R.drawable.ic_mode_light else R.drawable.ic_mode_dark
                    ),
                    contentDescription = stringResource(R.string.theme_toggle),
                    tint = AppColors.textSecondary
                )
            }
        }
    }
}
