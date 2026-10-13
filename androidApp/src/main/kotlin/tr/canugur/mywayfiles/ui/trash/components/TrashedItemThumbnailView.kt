package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.ThumbnailLoader
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * High-performance thumbnail loader specifically designed for TrashedItem in the Recycle Bin.
 */
@Composable
fun TrashedItemThumbnailView(
    item: TrashedItem,
    modifier: Modifier = Modifier
) {
    val extension = remember(item.name) {
        item.name.substringAfterLast('.', "").lowercase()
    }
    val isVideo = remember(extension, item.isDirectory) {
        !item.isDirectory && FileTypeClassifier.isVideo(extension)
    }
    val isVisualMedia = remember(extension, item.isDirectory) {
        !item.isDirectory && (FileTypeClassifier.isImage(extension) || isVideo)
    }

    val iconContainerShape = RoundedCornerShape(Dimensions.CornerRadiusSmall)

    if (isVisualMedia) {
        var thumbnailBitmap by remember(item.trashPath) {
            mutableStateOf(ThumbnailLoader.getFromCache(item.trashPath))
        }

        LaunchedEffect(item.trashPath) {
            if (thumbnailBitmap == null) {
                thumbnailBitmap = ThumbnailLoader.loadThumbnail(item.trashPath)
            }
        }

        val bitmap = thumbnailBitmap
        if (bitmap != null) {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(iconContainerShape)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = AppColors.cardStroke,
                        shape = iconContainerShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(iconContainerShape)
                    .background(AppColors.cardSurfaceSubtle)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = AppColors.cardStroke,
                        shape = iconContainerShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (isVideo) R.drawable.ic_video else R.drawable.ic_image),
                    contentDescription = null,
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }
        }
    } else {
        val iconRes = if (item.isDirectory) R.drawable.ic_folder else R.drawable.ic_file_generic
        val iconTint = if (item.isDirectory) AppColors.statusWarning else AppColors.textSecondary

        Box(
            modifier = modifier
                .size(38.dp)
                .clip(iconContainerShape)
                .background(AppColors.cardSurfaceSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(Dimensions.IconSizeDefault)
            )
        }
    }
}
