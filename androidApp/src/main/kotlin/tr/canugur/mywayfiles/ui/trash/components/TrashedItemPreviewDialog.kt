package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.BitmapDecoder
import tr.canugur.mywayfiles.core.image.ThumbnailLoader
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Inspection dialog for trashed files featuring visual preview, metadata details, and direct restore/deletion actions.
 */
@Composable
fun TrashedItemPreviewDialog(
    item: TrashedItem,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extension = remember(item.name) {
        item.name.substringAfterLast('.', "").lowercase()
    }
    val isImage = remember(extension, item.isDirectory) {
        !item.isDirectory && FileTypeClassifier.isImage(extension)
    }
    val isVideo = remember(extension, item.isDirectory) {
        !item.isDirectory && FileTypeClassifier.isVideo(extension)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurface,
        title = {
            Text(
                text = item.name,
                color = AppColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                if (isImage || isVideo) {
                    var previewBitmap by remember(item.trashPath) {
                        mutableStateOf<ImageBitmap?>(null)
                    }
                    var isLoading by remember(item.trashPath) { mutableStateOf(true) }

                    LaunchedEffect(item.trashPath) {
                        isLoading = true
                        previewBitmap = if (isImage) {
                            BitmapDecoder.decode(item.trashPath).getOrNull()?.imageBitmap
                        } else {
                            ThumbnailLoader.loadThumbnail(item.trashPath)
                        }
                        isLoading = false
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(Dimensions.CornerRadiusDefault))
                            .background(AppColors.cardSurfaceSubtle)
                            .border(
                                width = Dimensions.CardBorderWidth,
                                color = AppColors.cardStroke,
                                shape = RoundedCornerShape(Dimensions.CornerRadiusDefault)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = AppColors.brandAccent,
                                strokeWidth = Dimensions.SpacingMicro
                            )
                        } else {
                            val bitmap = previewBitmap
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(AppColors.mediaControlSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_play),
                                            contentDescription = null,
                                            tint = AppColors.mediaTextPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    painter = painterResource(if (isVideo) R.drawable.ic_video else R.drawable.ic_image),
                                    contentDescription = null,
                                    tint = AppColors.textMuted,
                                    modifier = Modifier.size(Dimensions.IconSizeLarge)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)) {
                    Text(
                        text = stringResource(R.string.original_location),
                        fontSize = 11.sp,
                        color = AppColors.textMuted
                    )
                    Text(
                        text = item.originalPath,
                        fontSize = 12.sp,
                        color = AppColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.properties_size),
                            fontSize = 11.sp,
                            color = AppColors.textMuted
                        )
                        Text(
                            text = ByteFormatter.format(item.sizeBytes),
                            fontSize = 12.sp,
                            color = AppColors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.deleted_time),
                            fontSize = 11.sp,
                            color = AppColors.textMuted
                        )
                        Text(
                            text = DateTimeFormatter.format(item.deletedAtMs),
                            fontSize = 12.sp,
                            color = AppColors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.brandAccent,
                    contentColor = AppColors.cardSurface
                ),
                shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_restore),
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.IconSizeSmall)
                )
                Spacer(modifier = Modifier.width(Dimensions.SpacingExtraSmall))
                Text(text = stringResource(R.string.action_restore), fontSize = 12.sp)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)) {
                TextButton(onClick = onDeletePermanently) {
                    Text(
                        text = stringResource(R.string.action_delete_permanently),
                        color = AppColors.statusCritical,
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.close_viewer),
                        color = AppColors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        modifier = modifier
    )
}
