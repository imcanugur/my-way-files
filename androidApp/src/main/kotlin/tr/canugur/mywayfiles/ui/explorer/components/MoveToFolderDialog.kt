package tr.canugur.mywayfiles.ui.explorer.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.ConflictResolution
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.data.preferences.SavedLocation
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import tr.canugur.mywayfiles.ui.common.ConflictResolutionBottomSheet
import tr.canugur.mywayfiles.ui.common.showOperationFeedback
import tr.canugur.mywayfiles.ui.explorer.FileOperation
import tr.canugur.mywayfiles.ui.explorer.MoveProgress
import tr.canugur.mywayfiles.ui.explorer.OperationFeedback
import java.io.File

data class MoveTargetRoot(val name: String, val path: String)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MoveToFolderDialog(
    itemsToMove: List<FileItem>,
    roots: List<MoveTargetRoot>,
    recentTargets: List<SavedLocation>,
    lastMoveTarget: SavedLocation?,
    preferLastMoveTarget: Boolean,
    initialRootPath: String,
    initialDirectoryPath: String,
    onLoadFolders: suspend (String) -> Result<List<FileItem>>,
    onCreateFolder: suspend (String, String) -> Result<Unit>,
    nestedConflictResolver: suspend (String, String, Boolean) -> tr.canugur.mywayfiles.ui.explorer.ConflictChoice = {
        _, _, _ -> tr.canugur.mywayfiles.ui.explorer.ConflictChoice(ConflictResolution.AUTO_RENAME)
    },
    onMove: suspend (
        List<FileItem>, String, ConflictResolution, Map<String, ConflictResolution>,
        (MoveProgress) -> Unit, suspend (String, String, Boolean) -> tr.canugur.mywayfiles.ui.explorer.ConflictChoice
    ) -> List<Result<Unit>>,
    onDismiss: () -> Unit
) {
    val safeRoots = remember(roots, initialRootPath) {
        roots.ifEmpty { listOf(MoveTargetRoot(File(initialRootPath).name, initialRootPath)) }
    }
    val availableRecentTargets = remember(recentTargets, safeRoots) {
        recentTargets.asSequence()
            .filter { target -> File(target.path).isDirectory }
            .filter { target -> safeRoots.any { storage -> isInsideRoot(storage.path, target.path) } }
            .distinctBy { target -> runCatching { File(target.path).canonicalPath }.getOrDefault(target.path) }
            .toList()
    }
    val preferredTargetPath = remember(lastMoveTarget, preferLastMoveTarget, safeRoots, itemsToMove) {
        lastMoveTarget?.path?.takeIf { path ->
            preferLastMoveTarget &&
                File(path).isDirectory &&
                safeRoots.any { isInsideRoot(it.path, path) } &&
                itemsToMove.none { item -> isInsideRoot(item.path, path) }
        }
    }
    val startingRoot = remember(safeRoots, initialRootPath, initialDirectoryPath) {
        safeRoots.firstOrNull { isInsideRoot(it.path, initialDirectoryPath) }
            ?: safeRoots.firstOrNull { it.path == initialRootPath }
            ?: safeRoots.first()
    }
    var root by remember(safeRoots, initialRootPath, initialDirectoryPath) {
        mutableStateOf(startingRoot)
    }
    var currentPath by remember(safeRoots, initialRootPath, initialDirectoryPath) {
        mutableStateOf(
            initialDirectoryPath.takeIf {
                isInsideRoot(startingRoot.path, it) && File(it).isDirectory
            } ?: startingRoot.path
        )
    }
    var selectedDestinationPath by remember(preferredTargetPath) {
        mutableStateOf(preferredTargetPath)
    }
    var folders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    var rootMenuExpanded by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var enterCreatedFolder by remember { mutableStateOf(false) }
    var creatingFolder by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var moveJob by remember { mutableStateOf<Job?>(null) }
    val moveProgressFlow = remember(itemsToMove) { MutableStateFlow<MoveProgress?>(null) }
    val moveProgress by moveProgressFlow.collectAsState()
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }
    var conflictsToResolve by remember { mutableStateOf<List<FileItem>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val folderListMaxHeight = minOf(Dimensions.MoveFolderListMaxHeight, screenHeight * 0.32f)

    LaunchedEffect(currentPath, reloadToken) {
        loading = true
        error = null
        if (!isInsideRoot(root.path, currentPath)) {
            error = context.getString(R.string.move_outside_storage)
            folders = emptyList()
        } else {
            val directoryResult = try {
                onLoadFolders(currentPath)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
            directoryResult
                .onSuccess { folders = it }
                .onFailure { error = it.localizedMessage ?: "" }
        }
        loading = false
    }

    if (!showCreateFolder) {
    AppModalBottomSheet(
        onDismissRequest = { if (!moving && !creatingFolder) onDismiss() },
        expandFullyOnShow = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            Text(stringResource(R.string.move_to_folder_title), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
                Text(
                    text = stringResource(R.string.move_items_count, itemsToMove.size),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary
                )
                val sourceFolders = itemsToMove.mapNotNull { File(it.path).parent }
                    .distinct().joinToString(" • ")
                Text(
                    text = stringResource(R.string.move_source, sourceFolders),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentPath != root.path) {
                        IconButton(
                            onClick = {
                                currentPath = File(currentPath).parentFile?.path ?: root.path
                                selectedDestinationPath = currentPath
                            },
                            enabled = !moving
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.navigate_up),
                                tint = AppColors.textPrimary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.move_destination),
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = AppColors.textSecondary
                            )
                            if (safeRoots.size > 1) {
                                androidx.compose.foundation.layout.Box {
                                    TextButton(onClick = { rootMenuExpanded = true }, enabled = !moving) {
                                        Text(root.name)
                                    }
                                    DropdownMenu(
                                        expanded = rootMenuExpanded,
                                        onDismissRequest = { rootMenuExpanded = false }
                                    ) {
                                        safeRoots.forEach { destination ->
                                            DropdownMenuItem(
                                                text = { Text(destination.name) },
                                                onClick = {
                                                    rootMenuExpanded = false
                                                    root = destination
                                                    currentPath = destination.path
                                                    selectedDestinationPath = destination.path
                                                    resultMessage = null
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = root.name,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                    color = AppColors.brandAccent
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedDestinationPath ?: currentPath,
                                modifier = Modifier.weight(1f),
                                softWrap = true,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = AppColors.textPrimary
                            )
                            if (selectedDestinationPath != null) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = stringResource(R.string.move_destination_selected),
                                    tint = AppColors.brandAccent
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { folderName = ""; enterCreatedFolder = false; showCreateFolder = true },
                    enabled = !moving && !loading
                ) { Text(stringResource(R.string.action_new_folder)) }

                when {
                    moving -> {
                        val currentProgress = moveProgress
                        val currentItemName = currentProgress
                            ?.let { itemsToMove.getOrNull(it.itemIndex)?.name }
                            ?: itemsToMove.firstOrNull()?.name.orEmpty()
                        val progressFraction = currentProgress?.takeIf { it.totalBytes > 0L }
                            ?.let { (it.copiedBytes.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) }

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Dimensions.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.move_progress_item,
                                    (currentProgress?.itemIndex ?: 0) + 1,
                                    currentProgress?.itemCount ?: itemsToMove.size,
                                    currentItemName
                                ),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = AppColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (progressFraction != null) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(Dimensions.SpacingMicro)
                                        .clip(CircleShape).background(AppColors.divider)
                                        .semantics {
                                            progressBarRangeInfo = ProgressBarRangeInfo(progressFraction, 0f..1f)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(progressFraction).fillMaxHeight()
                                            .background(AppColors.brandAccent)
                                    )
                                }
                                Text(
                                    text = stringResource(
                                        R.string.move_progress_bytes,
                                        Formatter.formatFileSize(context, currentProgress?.copiedBytes ?: 0L),
                                        Formatter.formatFileSize(context, currentProgress?.totalBytes ?: 0L),
                                        (progressFraction * 100f).toInt()
                                    ),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                    color = AppColors.textSecondary
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = AppColors.brandAccent
                                )
                            }
                        }
                    }
                    loading -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimensions.SpacingMedium),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                    error != null -> Column {
                        Text(
                            error?.takeIf { it.isNotBlank() } ?: stringResource(R.string.error_unknown),
                            color = AppColors.statusCritical
                        )
                        TextButton(onClick = { reloadToken++ }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                    resultMessage != null -> Text(
                        resultMessage.orEmpty(),
                        color = if (resultIsError) AppColors.statusCritical else AppColors.textPrimary
                    )
                    else -> LazyColumn(modifier = Modifier.heightIn(max = folderListMaxHeight)) {
                        if (availableRecentTargets.isNotEmpty()) {
                            item(key = "recent_targets_header") {
                                Text(
                                    stringResource(R.string.move_recent_targets),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                    color = AppColors.textSecondary,
                                    modifier = Modifier.padding(
                                        top = Dimensions.SpacingSmall,
                                        bottom = Dimensions.SpacingSmall
                                    )
                                )
                            }
                            items(
                                availableRecentTargets,
                                key = { "recent:${it.path}" }
                            ) { target ->
                                val isTargetSelected = selectedDestinationPath?.let { canonicalPathEquals(target.path, it) } ?: false
                                ListItem(
                                    headlineContent = {
                                        Text(target.name, softWrap = true)
                                    },
                                    supportingContent = {
                                        Text(
                                            text = target.storageName?.let { "$it • ${target.path}" } ?: target.path,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = AppColors.textSecondary
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_folder),
                                            contentDescription = null,
                                            tint = AppColors.brandAccent
                                        )
                                    },
                                    trailingContent = if (isTargetSelected) {
                                        {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_check),
                                                contentDescription = stringResource(R.string.move_destination_selected),
                                                tint = AppColors.brandAccent
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier
                                        .semantics { selected = isTargetSelected }
                                        .clickable(enabled = !moving) {
                                        val targetRoot = safeRoots.firstOrNull {
                                            isInsideRoot(it.path, target.path)
                                        }
                                        if (targetRoot != null) {
                                            root = targetRoot
                                            currentPath = target.path
                                            selectedDestinationPath = target.path
                                            resultMessage = null
                                            error = null
                                        }
                                    }
                                )
                            }
                        }
                        item(key = "folders_header") {
                            Text(
                                stringResource(R.string.move_browse_folders),
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                color = AppColors.textSecondary,
                                modifier = Modifier.padding(
                                    top = Dimensions.SpacingSmall,
                                    bottom = Dimensions.SpacingSmall
                                )
                            )
                        }
                        if (folders.isEmpty()) {
                            item(key = "no_folders") {
                                Text(stringResource(R.string.move_no_folders), color = AppColors.textSecondary)
                            }
                        }
                        items(folders, key = { "folder:${it.id}" }) { folder ->
                            val isSelfOrChild = itemsToMove.any { isInsideRoot(it.path, folder.path) }
                            ListItem(
                                headlineContent = { Text(folder.name, softWrap = true) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = if (isSelfOrChild) AppColors.textSecondary.copy(alpha = 0.5f) else AppColors.brandAccent
                                    )
                                },
                                modifier = Modifier.clickable(enabled = !isSelfOrChild && !moving) {
                                    if (isInsideRoot(root.path, folder.path)) {
                                        currentPath = folder.path
                                        selectedDestinationPath = folder.path
                                        resultMessage = null
                                    } else {
                                        error = context.getString(R.string.move_outside_storage)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                when {
                    moving -> TextButton(onClick = { moveJob?.cancel() }) {
                        Text(stringResource(R.string.operation_cancel))
                    }
                    resultMessage != null -> TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_done))
                    }
                    else -> TextButton(onClick = onDismiss, enabled = !creatingFolder) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                val destinationToMove = selectedDestinationPath ?: currentPath
                val isMovingInsideSelf = itemsToMove.any { isInsideRoot(it.path, destinationToMove) }

                fun executeMove(
                    targetDestination: String,
                    resolution: ConflictResolution,
                    itemResolutions: Map<String, ConflictResolution> = emptyMap()
                ) {
                    moving = true
                    moveProgressFlow.value = MoveProgress(0, itemsToMove.size, 0L, 0L)
                    moveJob = scope.launch {
                        try {
                            val results = try {
                                onMove(
                                    itemsToMove,
                                    targetDestination,
                                    resolution,
                                    itemResolutions,
                                    { progress -> moveProgressFlow.value = progress },
                                    nestedConflictResolver
                                )
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                listOf(Result.failure(failure))
                            }
                            val succeeded = results.count { it.isSuccess }
                            val failed = results.size - succeeded
                            if (failed == 0 && results.isNotEmpty()) {
                                onDismiss()
                            } else {
                                val detail = results.firstNotNullOfOrNull { it.exceptionOrNull()?.localizedMessage }
                                resultMessage = context.getString(
                                    R.string.move_partial_result,
                                    succeeded,
                                    results.size,
                                    detail ?: context.getString(R.string.error_unknown)
                                )
                                resultIsError = true
                            }
                        } catch (cancelled: CancellationException) {
                            resultMessage = context.getString(R.string.operation_cancelled_short)
                            resultIsError = true
                        } finally {
                            moving = false
                            moveJob = null
                        }
                    }
                }

                TextButton(
                    enabled = !loading && !moving && error == null && !isMovingInsideSelf,
                    onClick = {
                        if (!moving && !loading) {
                            val conflicts = itemsToMove.filter { item ->
                                val dest = File(destinationToMove, item.name)
                                dest.exists() && runCatching { dest.canonicalPath != File(item.path).canonicalPath }.getOrDefault(true)
                            }
                            if (conflicts.isNotEmpty()) {
                                conflictsToResolve = conflicts
                            } else {
                                executeMove(destinationToMove, ConflictResolution.AUTO_RENAME)
                            }
                        }
                    }
                ) {
                    Text(stringResource(if (moving) R.string.move_in_progress else R.string.action_move_here))
                }
            }
            }
        }
    }

    if (showCreateFolder) {
        val existingFolderNames = remember(folders) { folders.map { it.name }.toSet() }
        val trimmedFolderName = folderName.trim()
        val isValidFolderName = trimmedFolderName.isNotEmpty() && trimmedFolderName != "." && trimmedFolderName != ".." &&
            !trimmedFolderName.contains('/') && !trimmedFolderName.contains('\\')
        val isDuplicateFolder = isValidFolderName && existingFolderNames.any { it.equals(trimmedFolderName, ignoreCase = true) }
        val hasInvalidInput = folderName.isNotEmpty() && !isValidFolderName

        val suggestedFolderName = remember(trimmedFolderName, existingFolderNames) {
            val base = if (trimmedFolderName.isNotBlank()) trimmedFolderName else "New Folder"
            var count = 1
            while (existingFolderNames.any { it.equals("$base ($count)", ignoreCase = true) }) {
                count++
            }
            "$base ($count)"
        }

        AppModalBottomSheet(onDismissRequest = { if (!creatingFolder) showCreateFolder = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                    .padding(bottom = Dimensions.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                Text(stringResource(R.string.dialog_new_folder_title), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.dialog_new_folder_placeholder)) },
                    supportingText = when {
                        hasInvalidInput -> {
                            { Text(stringResource(R.string.invalid_file_name), color = AppColors.statusCritical) }
                        }
                        isDuplicateFolder -> {
                            { Text(stringResource(R.string.conflict_name_already_exists), color = AppColors.statusCritical) }
                        }
                        else -> null
                    },
                    isError = hasInvalidInput || isDuplicateFolder,
                    singleLine = true,
                    enabled = !creatingFolder,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isDuplicateFolder) {
                    AssistChip(
                        onClick = { folderName = suggestedFolderName },
                        label = {
                            Text(
                                stringResource(R.string.conflict_suggestion, suggestedFolderName),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_new_folder),
                                contentDescription = null,
                                tint = AppColors.brandAccent
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = AppColors.brandAccent.copy(alpha = 0.08f),
                            labelColor = AppColors.brandAccent
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !creatingFolder) { enterCreatedFolder = !enterCreatedFolder }
                ) {
                    Checkbox(
                        checked = enterCreatedFolder,
                        onCheckedChange = { enterCreatedFolder = it },
                        enabled = !creatingFolder
                    )
                    Text(
                        text = stringResource(R.string.dialog_new_folder_enter),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = AppColors.textPrimary
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showCreateFolder = false }, enabled = !creatingFolder) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        enabled = isValidFolderName && !isDuplicateFolder && !creatingFolder,
                        onClick = {
                            scope.launch {
                                creatingFolder = true
                                val result = try {
                                    onCreateFolder(currentPath, folderName.trim())
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Exception) {
                                    Result.failure(failure)
                                }
                                result.onSuccess {
                                    context.showOperationFeedback(OperationFeedback(FileOperation.CREATE_FOLDER, 1, 0))
                                    val newPath = File(currentPath, folderName.trim()).path
                                    showCreateFolder = false
                                    selectedDestinationPath = newPath
                                    if (enterCreatedFolder) {
                                        currentPath = newPath
                                    } else {
                                        reloadToken++
                                    }
                                    resultMessage = null
                                    error = null
                                }.onFailure {
                                    context.showOperationFeedback(
                                        OperationFeedback(FileOperation.CREATE_FOLDER, 0, 1, it.localizedMessage)
                                    )
                                    error = it.localizedMessage ?: ""
                                    showCreateFolder = false
                                }
                                creatingFolder = false
                            }
                        }
                    ) { Text(stringResource(R.string.action_create)) }
                }
            }
        }
    }

    val currentConflicts = conflictsToResolve
    if (currentConflicts != null) {
        ConflictResolutionBottomSheet(
            conflictingNames = currentConflicts.map { it.name },
            folderConflictCount = currentConflicts.count { it.isDirectory },
            fileConflictCount = currentConflicts.count { !it.isDirectory },
            allowMergeFolders = currentConflicts.any { it.isDirectory },
            allowIndividualChoices = currentConflicts.size > 1,
            itemKeys = currentConflicts.map { it.path },
            folderNames = currentConflicts.filter { it.isDirectory }.map { it.name }.toSet(),
            onResolveItems = { resolution, itemResolutions ->
                val destinationToMove = selectedDestinationPath ?: currentPath
                fun exec(
                    target: String,
                    res: ConflictResolution,
                    itemResolutions: Map<String, ConflictResolution> = emptyMap()
                ) {
                    moving = true
                    moveProgressFlow.value = MoveProgress(0, itemsToMove.size, 0L, 0L)
                    moveJob = scope.launch {
                        try {
                            val results = try {
                                onMove(
                                    itemsToMove,
                                    target,
                                    res,
                                    itemResolutions,
                                    { progress -> moveProgressFlow.value = progress },
                                    { source, destination, allowMerge ->
                                        // The parent Explorer owns the prompt state and resumes this move after a choice.
                                        nestedConflictResolver(source, destination, allowMerge)
                                    }
                                )
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                listOf(Result.failure(failure))
                            }
                            val succeeded = results.count { it.isSuccess }
                            val failed = results.size - succeeded
                            if (failed == 0 && results.isNotEmpty()) {
                                onDismiss()
                            } else {
                                val detail = results.firstNotNullOfOrNull { it.exceptionOrNull()?.localizedMessage }
                                resultMessage = context.getString(
                                    R.string.move_partial_result,
                                    succeeded,
                                    results.size,
                                    detail ?: context.getString(R.string.error_unknown)
                                )
                                resultIsError = true
                            }
                        } catch (cancelled: CancellationException) {
                            resultMessage = context.getString(R.string.operation_cancelled_short)
                            resultIsError = true
                        } finally {
                            moving = false
                            moveJob = null
                        }
                    }
                }
                exec(destinationToMove, resolution, itemResolutions)
                conflictsToResolve = null
            },
            onResolve = { conflictsToResolve = null },
            onDismiss = {
                conflictsToResolve = null
            }
        )
    }
}

private fun isInsideRoot(rootPath: String, candidatePath: String): Boolean = runCatching {
    val root = File(rootPath).canonicalPath.trimEnd(File.separatorChar)
    val candidate = File(candidatePath).canonicalPath
    candidate == root || candidate.startsWith(root + File.separator)
}.getOrDefault(false)

private fun canonicalPathEquals(firstPath: String, secondPath: String): Boolean =
    runCatching { File(firstPath).canonicalPath == File(secondPath).canonicalPath }
        .getOrDefault(firstPath == secondPath)
