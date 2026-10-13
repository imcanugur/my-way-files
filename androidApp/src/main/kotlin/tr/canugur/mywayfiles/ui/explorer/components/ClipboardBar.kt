package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.explorer.Clipboard
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

@Composable
fun ClipboardBar(
    clipboard: Clipboard,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimensions.CornerRadiusMedium)

    Surface(
        color = AppColors.cardSurface,
        shape = shape,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = Dimensions.CardBorderWidth, color = AppColors.brandAccent, shape = shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(if (clipboard.isCut) R.drawable.ic_cut else R.drawable.ic_copy),
                    contentDescription = null,
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )

                Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))

                Column {
                    val actionLabel = if (clipboard.isCut) {
                        stringResource(R.string.action_cut)
                    } else {
                        stringResource(R.string.action_copy)
                    }

                    Text(
                        text = "$actionLabel: ${clipboard.item.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))

            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingExtraSmall)) {
                Button(
                    onClick = onPaste,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.brandAccent,
                        contentColor = AppColors.cardSurface
                    ),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusSmall),
                    contentPadding = PaddingValues(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingExtraSmall)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_paste),
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.IconSizeSmall)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.SpacingExtraSmall))
                    Text(
                        text = stringResource(R.string.action_paste),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.textSecondary)
                ) {
                    Text(text = stringResource(R.string.action_cancel), fontSize = 12.sp)
                }
            }
        }
    }
}
