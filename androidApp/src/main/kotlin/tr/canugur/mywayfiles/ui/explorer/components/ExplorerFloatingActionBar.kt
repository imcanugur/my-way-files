package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Modern floating action capsule anchored in the comfortable thumb zone at the bottom of the screen.
 * Replaces the hard-to-reach top-right 3-dots menu with direct, instant access to primary actions.
 */
@Composable
fun ExplorerFloatingActionBar(
    onCreateFolder: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    sortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = Dimensions.SpacingSmall),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.widthIn(max = maxWidth - Dimensions.SpacingLarge),
            color = AppColors.cardSurface,
            contentColor = AppColors.textPrimary,
            shape = RoundedCornerShape(Dimensions.CornerRadiusPill),
            shadowElevation = 4.dp,
            tonalElevation = 2.dp,
            border = BorderStroke(Dimensions.CardBorderWidth, AppColors.cardStroke)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = Dimensions.SpacingDefault,
                    vertical = Dimensions.SpacingExtraSmall
                ),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // New Folder (Primary CTA)
                TextButton(
                    onClick = onCreateFolder,
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SpacingSmall,
                        vertical = Dimensions.SpacingMicro
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_new_folder),
                        contentDescription = null,
                        tint = AppColors.brandAccent,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                    Text(
                        text = stringResource(R.string.action_new_folder),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.brandAccent
                    )
                }

                // Subtle vertical divider between actions
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(AppColors.divider)
                )

                // Sort Dropdown Anchor
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.size(Dimensions.ActionButtonSize)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sort),
                            contentDescription = stringResource(R.string.sort_by),
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(Dimensions.IconSizeMedium)
                        )
                    }
                    ExplorerOptionsMenu(
                        expanded = sortMenuExpanded,
                        currentSortOption = sortOption,
                        onDismissRequest = { sortMenuExpanded = false },
                        onCreateFolderClick = onCreateFolder,
                        onEnterSelectionMode = onEnterSelectionMode,
                        onSortOptionSelected = onSortOptionSelected,
                        onRefreshClick = {},
                        sortOnly = true
                    )
                }

                // Selection Mode Button
                IconButton(
                    onClick = onEnterSelectionMode,
                    modifier = Modifier.size(Dimensions.ActionButtonSize)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.selection_mode),
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }
            }
        }
    }
}
