package tr.canugur.mywayfiles.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.image.BitmapDecoder
import tr.canugur.mywayfiles.core.image.DecodedBitmapResult
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.explorer.components.TrashedItemThumbnailView
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: RecycleBinViewModel = viewModel(
        factory = RecycleBinViewModel.provideFactory(context)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemToDeletePermanently by remember { mutableStateOf<TrashedItem?>(null) }
    var itemToPreview by remember { mutableStateOf<TrashedItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadItems()
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = AppColors.canvasBackground,
        topBar = {
            Surface(
                color = AppColors.cardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .border(width = Dimensions.CardBorderWidth, color = AppColors.cardStroke)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.SpacingDefault, vertical = Dimensions.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimensions.ActionButtonSize)
                                .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                                .background(AppColors.cardSurfaceSubtle)
                                .border(
                                    width = Dimensions.CardBorderWidth,
                                    color = AppColors.cardStroke,
                                    shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                                )
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.navigate_up),
                                tint = AppColors.textPrimary,
                                modifier = Modifier.size(Dimensions.IconSizeMedium)
                            )
                        }

                        Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                        Column {
                            Text(
                                text = stringResource(R.string.recycle_bin),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimary
                            )

                            val successState = state as? RecycleBinUiState.Success
                            if (successState != null) {
                                Text(
                                    text = "${successState.items.size} öge • ${ByteFormatter.format(successState.totalBytes)}",
                                    fontSize = 11.sp,
                                    color = AppColors.textMuted
                                )
                            }
                        }
                    }

                    // Empty Trash Action Button
                    val isSuccessWithItems = (state as? RecycleBinUiState.Success)?.items?.isNotEmpty() == true
                    if (isSuccessWithItems) {
                        Button(
                            onClick = { showEmptyTrashConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.statusCritical.copy(alpha = 0.12f),
                                contentColor = AppColors.statusCritical
                            ),
                            shape = RoundedCornerShape(Dimensions.CornerRadiusSmall),
                            contentPadding = PaddingValues(horizontal = Dimensions.SpacingSmall, vertical = Dimensions.SpacingExtraSmall)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_empty_trash),
                                contentDescription = null,
                                modifier = Modifier.size(Dimensions.IconSizeSmall)
                            )
                            Spacer(modifier = Modifier.width(Dimensions.SpacingExtraSmall))
                            Text(
                                text = stringResource(R.string.action_empty_recycle_bin),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (val s = state) {
            is RecycleBinUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppColors.brandAccent,
                        strokeWidth = Dimensions.SpacingMicro
                    )
                }
            }

            is RecycleBinUiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Dimensions.SpacingHuge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_recycle_bin),
                        contentDescription = null,
                        tint = AppColors.textMuted,
                        modifier = Modifier.size(Dimensions.IconSizeLarge)
                    )
                    Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
                    Text(
                        text = stringResource(R.string.recycle_bin_empty),
                        fontSize = 14.sp,
                        color = AppColors.textMuted
                    )
                }
            }

            is RecycleBinUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.message,
                        color = AppColors.statusCritical,
                        fontSize = 13.sp
                    )
                }
            }

            is RecycleBinUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SpacingDefault,
                        vertical = Dimensions.SpacingMedium
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    items(s.items, key = { it.id }) { item ->
                        TrashedItemRow(
                            item = item,
                            onClick = { itemToPreview = item },
                            onRestore = { viewModel.onEvent(RecycleBinUiEvent.Restore(item)) },
                            onDeletePermanently = { itemToDeletePermanently = item }
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showEmptyTrashConfirm) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashConfirm = false },
                containerColor = AppColors.cardSurface,
                title = {
                    Text(
                        text = stringResource(R.string.action_empty_recycle_bin),
                        color = AppColors.statusCritical,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.dialog_empty_recycle_bin_confirm),
                        color = AppColors.textPrimary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onEvent(RecycleBinUiEvent.EmptyTrash)
                            showEmptyTrashConfirm = false
                        }
                    ) {
                        Text(text = stringResource(R.string.action_empty_recycle_bin), color = AppColors.statusCritical)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashConfirm = false }) {
                        Text(text = stringResource(R.string.action_cancel), color = AppColors.textSecondary)
                    }
                }
            )
        }

        val permItem = itemToDeletePermanently
        if (permItem != null) {
            AlertDialog(
                onDismissRequest = { itemToDeletePermanently = null },
                containerColor = AppColors.cardSurface,
                title = {
                    Text(
                        text = stringResource(R.string.action_delete_permanently),
                        color = AppColors.statusCritical,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.dialog_delete_confirm, permItem.name),
                        color = AppColors.textPrimary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onEvent(RecycleBinUiEvent.DeletePermanently(permItem))
                            itemToDeletePermanently = null
                        }
                    ) {
                        Text(text = stringResource(R.string.action_delete), color = AppColors.statusCritical)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDeletePermanently = null }) {
                        Text(text = stringResource(R.string.action_cancel), color = AppColors.textSecondary)
                    }
                }
            )
        }

        val previewItem = itemToPreview
        if (previewItem != null) {
            TrashedItemPreviewDialog(
                item = previewItem,
                onRestore = {
                    viewModel.onEvent(RecycleBinUiEvent.Restore(previewItem))
                    itemToPreview = null
                },
                onDeletePermanently = {
                    itemToDeletePermanently = previewItem
                    itemToPreview = null
                },
                onDismiss = { itemToPreview = null }
            )
        }
    }
}

@Composable
private fun TrashedItemRow(
    item: TrashedItem,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimensions.CornerRadiusDefault)
    val extension = remember(item.name) {
        item.name.substringAfterLast('.', "").lowercase()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurface)
            .border(width = Dimensions.CardBorderWidth, color = AppColors.cardStroke, shape = shape)
            .clickable { onClick() }
            .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High-performance cached thumbnail or folder/file icon
        TrashedItemThumbnailView(item = item)

        Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))

            Text(
                text = "${stringResource(R.string.original_location)}: ${item.originalPath}",
                fontSize = 11.sp,
                color = AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${ByteFormatter.format(item.sizeBytes)} • ${DateTimeFormatter.format(item.deletedAtMs)}",
                fontSize = 10.sp,
                color = AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Extension Badge
        if (!item.isDirectory && extension.isNotEmpty()) {
            Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                    .background(AppColors.cardSurfaceSubtle)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = AppColors.cardStroke,
                        shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                    )
                    .padding(
                        horizontal = Dimensions.SpacingExtraSmall,
                        vertical = Dimensions.SpacingMicro
                    )
            ) {
                Text(
                    text = extension.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))

        // Actions: Restore & Permanent Delete
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)) {
            IconButton(
                onClick = onRestore,
                modifier = Modifier.size(Dimensions.ActionButtonSize)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_restore),
                    contentDescription = stringResource(R.string.action_restore),
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }

            IconButton(
                onClick = onDeletePermanently,
                modifier = Modifier.size(Dimensions.ActionButtonSize)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_delete_permanently),
                    tint = AppColors.statusCritical,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }
        }
    }
}

@Composable
private fun TrashedItemPreviewDialog(
    item: TrashedItem,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    onDismiss: () -> Unit
) {
    val extension = remember(item.name) {
        item.name.substringAfterLast('.', "").lowercase()
    }
    val isImage = remember(extension, item.isDirectory) {
        !item.isDirectory && FileTypeClassifier.isImage(extension)
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
                if (isImage) {
                    var decodedResult by remember(item.trashPath) {
                        mutableStateOf<DecodedBitmapResult?>(null)
                    }
                    var isLoading by remember(item.trashPath) { mutableStateOf(true) }

                    LaunchedEffect(item.trashPath) {
                        isLoading = true
                        val res = BitmapDecoder.decode(item.trashPath)
                        decodedResult = res.getOrNull()
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
                            val bitmap = decodedResult?.imageBitmap
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_image),
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
        }
    )
}
