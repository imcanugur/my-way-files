package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.data.preferences.SavedLocation
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

@Composable
fun HomeSavedLocationsSection(
    title: String,
    locations: List<SavedLocation>,
    onLocationClick: (SavedLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    if (locations.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.textPrimary,
            modifier = Modifier.padding(vertical = Dimensions.SpacingSmall)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            items(locations, key = { it.path }) { loc ->
                Surface(
                    modifier = Modifier
                        .widthIn(min = 120.dp, max = 160.dp)
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
                        .clickable { onLocationClick(loc) },
                    color = AppColors.cardSurface,
                    shape = RoundedCornerShape(Dimensions.CornerRadiusMedium),
                    border = BorderStroke(Dimensions.CardBorderWidth, AppColors.cardStroke),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = Dimensions.SpacingDefault,
                            vertical = Dimensions.SpacingSmall
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (loc.isDirectory) R.drawable.ic_folder else R.drawable.ic_file_generic
                            ),
                            contentDescription = null,
                            tint = if (loc.isDirectory) AppColors.statusWarning else AppColors.textSecondary,
                            modifier = Modifier.size(Dimensions.IconSizeMedium)
                        )
                        Text(
                            text = loc.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
