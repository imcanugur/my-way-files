package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

data class QuickCategory(
    val titleRes: Int,
    val relativePath: String,
    val iconRes: Int,
    val accentColor: Color
)

@Composable
fun rememberDefaultQuickCategories(): List<QuickCategory> {
    val brand = AppColors.brandAccent
    val warning = AppColors.statusWarning
    val critical = AppColors.statusCritical
    return androidx.compose.runtime.remember(brand, warning, critical) {
        listOf(
            QuickCategory(R.string.category_downloads, "Download", R.drawable.ic_download, brand),
            QuickCategory(R.string.category_images, "DCIM", R.drawable.ic_image, warning),
            QuickCategory(R.string.category_videos, "Movies", R.drawable.ic_video, brand),
            QuickCategory(R.string.category_audio, "Music", R.drawable.ic_audio, brand),
            QuickCategory(R.string.category_documents, "Documents", R.drawable.ic_document, critical)
        )
    }
}

/**
 * Modern Google Files-style Quick Access category cards on the home screen.
 */
@Composable
fun HomeQuickCategories(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<QuickCategory> = rememberDefaultQuickCategories()
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
    ) {
        items(categories, key = { it.relativePath }) { cat ->
            Surface(
                modifier = Modifier
                    .width(110.dp)
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
                    .clickable { onCategoryClick(cat.relativePath) },
                color = AppColors.cardSurface,
                shape = RoundedCornerShape(Dimensions.CornerRadiusMedium),
                border = BorderStroke(Dimensions.CardBorderWidth, AppColors.cardStroke),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.SpacingDefault),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                            .background(cat.accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(cat.iconRes),
                            contentDescription = null,
                            tint = cat.accentColor,
                            modifier = Modifier.size(Dimensions.IconSizeMedium)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                    Text(
                        text = stringResource(cat.titleRes),
                        style = MaterialTheme.typography.labelMedium,
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
