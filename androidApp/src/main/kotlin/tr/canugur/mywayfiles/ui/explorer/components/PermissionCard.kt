package tr.canugur.mywayfiles.ui.explorer.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Enterprise permission prompt guiding users to grant required file management access.
 */
@Composable
fun PermissionCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(Dimensions.CornerRadiusMedium)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurface)
            .border(
                width = Dimensions.CardBorderWidth,
                color = AppColors.cardStroke,
                shape = shape
            )
            .padding(Dimensions.SpacingLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_folder),
                contentDescription = null,
                tint = AppColors.brandAccent,
                modifier = Modifier.size(Dimensions.IconSizeLarge)
            )

            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.permission_message),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    try {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        } else {
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val fallback = Intent(Settings.ACTION_SETTINGS)
                        context.startActivity(fallback)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.brandAccent,
                    contentColor = AppColors.cardSurface
                ),
                shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
            ) {
                Text(
                    text = stringResource(R.string.permission_button),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
