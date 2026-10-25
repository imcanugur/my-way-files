package tr.canugur.mywayfiles.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.ConflictResolution
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * BottomSheet presented when a file collision occurs during move, copy, or paste operations.
 * Opens fully expanded without requiring unnecessary scrolling.
 */
@Composable
fun ConflictResolutionBottomSheet(
    conflictingNames: List<String>,
    onResolve: (ConflictResolution) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(ConflictResolution.AUTO_RENAME) }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        expandFullyOnShow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.brandAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_rename),
                        contentDescription = null,
                        tint = AppColors.brandAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.conflict_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (conflictingNames.size == 1) {
                            stringResource(R.string.conflict_dialog_subtitle_single, conflictingNames.first())
                        } else {
                            stringResource(R.string.conflict_dialog_subtitle_multiple, conflictingNames.size)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Options List
            ConflictOptionCard(
                title = stringResource(R.string.conflict_auto_rename),
                description = stringResource(R.string.conflict_auto_rename_desc),
                iconRes = R.drawable.ic_edit_rename,
                isSelected = selectedOption == ConflictResolution.AUTO_RENAME,
                onClick = { selectedOption = ConflictResolution.AUTO_RENAME }
            )

            ConflictOptionCard(
                title = stringResource(R.string.conflict_overwrite),
                description = stringResource(R.string.conflict_overwrite_desc),
                iconRes = R.drawable.ic_check,
                isSelected = selectedOption == ConflictResolution.OVERWRITE,
                onClick = { selectedOption = ConflictResolution.OVERWRITE }
            )

            ConflictOptionCard(
                title = stringResource(R.string.conflict_skip),
                description = stringResource(R.string.conflict_skip_desc),
                iconRes = R.drawable.ic_arrow_back,
                isSelected = selectedOption == ConflictResolution.SKIP,
                onClick = { selectedOption = ConflictResolution.SKIP }
            )

            Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = Dimensions.ActionButtonSize)
                ) {
                    Text(stringResource(R.string.action_cancel), color = AppColors.textSecondary)
                }

                Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                Button(
                    onClick = {
                        onResolve(selectedOption)
                        onDismiss()
                    },
                    modifier = Modifier.heightIn(min = Dimensions.ActionButtonSize),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusPill),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.brandAccent)
                ) {
                    Text(stringResource(R.string.conflict_apply_button))
                }
            }
        }
    }
}

@Composable
private fun ConflictOptionCard(
    title: String,
    description: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.brandAccent else AppColors.cardStroke,
        label = "conflictCardBorder"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.brandAccent.copy(alpha = 0.06f) else AppColors.cardSurfaceSubtle,
        label = "conflictCardBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(Dimensions.CornerRadiusMedium)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (isSelected) AppColors.brandAccent else AppColors.textSecondary,
            modifier = Modifier.size(22.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.textPrimary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AppColors.brandAccent,
                unselectedColor = AppColors.textSecondary
            )
        )
    }
}
