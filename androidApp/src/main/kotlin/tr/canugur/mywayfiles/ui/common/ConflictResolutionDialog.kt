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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
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
    folderConflictCount: Int = 0,
    fileConflictCount: Int = (conflictingNames.size - folderConflictCount).coerceAtLeast(0),
    allowMergeFolders: Boolean = folderConflictCount > 0,
    contextDescription: String? = null,
    allowApplyToRemaining: Boolean = false,
    allowIndividualChoices: Boolean = false,
    itemKeys: List<String> = conflictingNames,
    folderNames: Set<String> = emptySet(),
    onResolveChoice: ((ConflictResolution, Boolean) -> Unit)? = null,
    onResolveItems: ((ConflictResolution, Map<String, ConflictResolution>) -> Unit)? = null,
    onResolve: (ConflictResolution) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(ConflictResolution.AUTO_RENAME) }
    var applyToRemaining by remember { mutableStateOf(false) }
    var individualMode by remember(conflictingNames) { mutableStateOf(false) }
    val perItemChoices = remember(conflictingNames) { mutableStateMapOf<String, ConflictResolution>() }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        expandFullyOnShow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
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

            contextDescription?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary
                )
            }
            if (allowIndividualChoices) {
                TextButton(
                    onClick = { individualMode = !individualMode },
                    modifier = Modifier.heightIn(min = Dimensions.ActionButtonSize)
                ) {
                    Text(
                        stringResource(
                            if (individualMode) R.string.conflict_use_one_choice
                            else R.string.conflict_choose_individually
                        )
                    )
                }
            }
            if (conflictingNames.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
                if (folderConflictCount > 0) {
                    Text(
                        text = stringResource(R.string.conflict_folder_count, folderConflictCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.textSecondary
                    )
                }
                if (fileConflictCount > 0) {
                    Text(
                        text = stringResource(R.string.conflict_file_count, fileConflictCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.textSecondary
                    )
                }
            }
            if (individualMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                        .background(AppColors.cardSurfaceSubtle)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)
                ) {
                    conflictingNames.forEachIndexed { index, name ->
                        val key = itemKeys.getOrNull(index) ?: name
                        val isFolder = name in folderNames
                        val defaultForItem = if (selectedOption == ConflictResolution.MERGE && !isFolder) {
                            ConflictResolution.AUTO_RENAME
                        } else selectedOption
                        ConflictItemChoiceRow(
                            name = name,
                            isFolder = isFolder,
                            selected = perItemChoices[key] ?: defaultForItem,
                            allowMerge = isFolder && allowMergeFolders,
                            onSelected = { perItemChoices[key] = it }
                        )
                    }
                }
            } else if (conflictingNames.size > 1) {
                conflictingNames.take(2).forEach { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textMuted,
                        maxLines = 1
                    )
                }
                if (conflictingNames.size > 2) {
                    Text(
                        text = stringResource(R.string.conflict_more_items, conflictingNames.size - 2),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (!individualMode) {
                if (allowMergeFolders) {
                    ConflictOptionCard(
                        title = stringResource(R.string.conflict_merge_folders),
                        description = stringResource(R.string.conflict_merge_folders_desc),
                        iconRes = R.drawable.ic_folder,
                        isSelected = selectedOption == ConflictResolution.MERGE,
                        onClick = { selectedOption = ConflictResolution.MERGE }
                    )
                }

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
            }

            if (allowApplyToRemaining) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyToRemaining = !applyToRemaining },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = applyToRemaining, onCheckedChange = { applyToRemaining = it })
                    Text(
                        text = stringResource(R.string.conflict_apply_remaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textPrimary
                    )
                }
            }

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
                        when {
                            onResolveItems != null -> onResolveItems(
                                selectedOption,
                                if (individualMode) perItemChoices.toMap() else emptyMap()
                            )
                            onResolveChoice != null -> onResolveChoice(selectedOption, applyToRemaining)
                            else -> onResolve(selectedOption)
                        }
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
private fun ConflictItemChoiceRow(
    name: String,
    isFolder: Boolean,
    selected: ConflictResolution,
    allowMerge: Boolean,
    onSelected: (ConflictResolution) -> Unit
) {
    var expanded by remember(name) { mutableStateOf(false) }
    val actionTitle = when (selected) {
        ConflictResolution.AUTO_RENAME -> stringResource(R.string.conflict_auto_rename_short)
        ConflictResolution.OVERWRITE -> stringResource(R.string.conflict_overwrite)
        ConflictResolution.SKIP -> stringResource(R.string.conflict_skip)
        ConflictResolution.MERGE -> stringResource(R.string.conflict_merge_folders)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(if (isFolder) R.drawable.ic_folder else R.drawable.ic_document),
            contentDescription = null,
            tint = if (isFolder) AppColors.brandAccent else AppColors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Dimensions.SpacingSmall))
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textPrimary,
            maxLines = 1
        )
        Box {
            TextButton(onClick = { expanded = true }) { Text(actionTitle, maxLines = 1) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (allowMerge) DropdownMenuItem(
                    text = { Text(stringResource(R.string.conflict_merge_folders)) },
                    onClick = { expanded = false; onSelected(ConflictResolution.MERGE) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.conflict_auto_rename)) },
                    onClick = { expanded = false; onSelected(ConflictResolution.AUTO_RENAME) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.conflict_overwrite)) },
                    onClick = { expanded = false; onSelected(ConflictResolution.OVERWRITE) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.conflict_skip)) },
                    onClick = { expanded = false; onSelected(ConflictResolution.SKIP) }
                )
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
