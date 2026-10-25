package tr.canugur.mywayfiles.ui.explorer.components

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.ThumbnailLoader
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Thumbnail component rendering asynchronously loaded, cached image thumbnails
 * or vector file/folder indicators with thematic color-coding.
 */
@Composable
fun FileThumbnailView(
    item: FileItem,
    modifier: Modifier = Modifier,
    thumbnailSize: Dp = 36.dp
) {
    val isVideo = remember(item.extension, item.isDirectory) {
        FileTypeClassifier.isVideo(item)
    }
    val isVisualMedia = remember(item.extension, item.isDirectory) {
        FileTypeClassifier.isImage(item) || isVideo
    }

    val iconContainerShape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
    val containerModifier = if (thumbnailSize == 36.dp) {
        modifier.size(thumbnailSize)
    } else {
        modifier.fillMaxSize()
    }

    if (isVisualMedia) {
        var thumbnailBitmap by remember(item.path) {
            mutableStateOf(ThumbnailLoader.getFromCache(item.path))
        }

        LaunchedEffect(item.path) {
            if (thumbnailBitmap == null) {
                thumbnailBitmap = ThumbnailLoader.loadThumbnail(item.path)
            }
        }

        val bitmap = thumbnailBitmap
        if (bitmap != null) {
            Box(
                modifier = containerModifier
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
                            .background(AppColors.mediaControlSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play),
                            contentDescription = null,
                            tint = AppColors.mediaTextPrimary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = containerModifier
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
                    modifier = Modifier.size(if (thumbnailSize >= 72.dp) 48.dp else Dimensions.IconSizeMedium)
                )
            }
        }
    } else {
        val (iconRes, iconTint) = when {
            item.isDirectory -> R.drawable.ic_folder to AppColors.statusWarning
            FileTypeClassifier.isAudio(item) -> R.drawable.ic_audio to AppColors.brandAccent
            FileTypeClassifier.isDocument(item) -> {
                if (item.extension.equals("pdf", ignoreCase = true)) {
                    R.drawable.ic_document to AppColors.statusCritical
                } else {
                    R.drawable.ic_document to AppColors.brandAccent
                }
            }
            FileTypeClassifier.isArchive(item) -> R.drawable.ic_archive to AppColors.statusWarning
            FileTypeClassifier.isApk(item) -> R.drawable.ic_apk to AppColors.brandAccent
            else -> R.drawable.ic_file_generic to AppColors.textSecondary
        }

        val backgroundMod = if (thumbnailSize >= 72.dp && !item.isDirectory) {
            containerModifier
                .clip(iconContainerShape)
                .background(iconTint.copy(alpha = 0.08f))
                .border(
                    width = Dimensions.CardBorderWidth,
                    color = iconTint.copy(alpha = 0.2f),
                    shape = iconContainerShape
                )
        } else {
            containerModifier
        }

        Box(
            modifier = backgroundMod,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (thumbnailSize >= 72.dp) 48.dp else Dimensions.IconSizeDefault)
            )
        }
    }
}

/**
 * Forwarding alias for TrashedItemThumbnailView (migrated to tr.canugur.mywayfiles.ui.trash.components).
 */
@Composable
fun TrashedItemThumbnailView(
    item: tr.canugur.mywayfiles.domain.model.TrashedItem,
    modifier: Modifier = Modifier
) {
    tr.canugur.mywayfiles.ui.trash.components.TrashedItemThumbnailView(
        item = item,
        modifier = modifier
    )
}
