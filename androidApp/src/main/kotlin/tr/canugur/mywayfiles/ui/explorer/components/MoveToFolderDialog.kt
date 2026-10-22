package tr.canugur.mywayfiles.ui.explorer.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.data.preferences.SavedLocation
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import java.io.File

data class MoveTargetRoot(val name: String, val path: String)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MoveToFolderDialog(
    itemsToMove: List<FileItem>,
    roots: List<MoveTargetRoot>,
    recentTargets: List<SavedLocation>,
    initialRootPath: String,
    onLoadFolders: suspend (String) -> Result<List<FileItem>>,
    onCreateFolder: suspend (String, String) -> Result<Unit>,
    onMove: suspend (List<FileItem>, String) -> List<Result<Unit>>,
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
            .take(5)
            .toList()
    }
    var root by remember(safeRoots, initialRootPath) {
        mutableStateOf(safeRoots.firstOrNull { it.path == initialRootPath } ?: safeRoots.first())
    }
    var currentPath by remember(root) { mutableStateOf(root.path) }
    var folders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    var rootMenuExpanded by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var creatingFolder by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
    AppModalBottomSheet(onDismissRequest = { if (!moving && !creatingFolder) onDismiss() }) {
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
                        IconButton(onClick = {
                            currentPath = File(currentPath).parentFile?.path ?: root.path
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.navigate_up),
                                tint = AppColors.textPrimary
                            )
                        }
                    }
                    Column(modifier = Modifier.widthIn(max = 160.dp)) {
                        Text(
                            text = stringResource(R.string.move_destination),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            color = AppColors.textSecondary
                        )
                        Text(
                            text = currentPath,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = AppColors.textPrimary
                        )
                    }
                    if (safeRoots.size > 1) {
                        androidx.compose.foundation.layout.Box {
                            TextButton(onClick = { rootMenuExpanded = true }) {
                                Text(stringResource(R.string.move_storage))
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
                                            resultMessage = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { folderName = ""; showCreateFolder = true },
                    enabled = !moving && !loading
                ) { Text(stringResource(R.string.action_new_folder)) }

                when {
                    moving -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimensions.SpacingMedium),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text(
                            stringResource(R.string.move_in_progress),
                            modifier = Modifier.padding(start = Dimensions.SpacingMedium)
                        )
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
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
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
                                ListItem(
                                    headlineContent = {
                                        Text(target.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    },
                                    supportingContent = {
                                        Text(
                                            target.storageName ?: target.path,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_folder),
                                            contentDescription = null,
                                            tint = AppColors.brandAccent
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        val targetRoot = safeRoots.firstOrNull {
                                            isInsideRoot(it.path, target.path)
                                        }
                                        if (targetRoot != null) {
                                            root = targetRoot
                                            currentPath = target.path
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
                            ListItem(
                                headlineContent = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = AppColors.brandAccent
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (isInsideRoot(root.path, folder.path)) {
                                        currentPath = folder.path
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
                if (resultMessage != null) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
                } else {
                    TextButton(onClick = onDismiss, enabled = !moving && !creatingFolder) {
                        Text(stringResource(R.string.action_cancel))
                    }
                TextButton(
                    enabled = !loading && !moving && error == null,
                    onClick = {
                        scope.launch {
                            moving = true
                            val results = try {
                                onMove(itemsToMove, currentPath)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                listOf(Result.failure(failure))
                            }
                            val succeeded = results.count { it.isSuccess }
                            val failed = results.size - succeeded
                            if (failed == 0 && results.isNotEmpty()) {
                                val message = context.getString(
                                    R.string.move_completed_count,
                                    succeeded,
                                    results.size
                                )
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                moving = false
                                onDismiss()
                                return@launch
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
                            moving = false
                        }
                    }
                ) { Text(stringResource(R.string.action_move_here)) }
            }
            }
        }
    }
    }

    if (showCreateFolder) {
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
                    singleLine = true,
                    enabled = !creatingFolder
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showCreateFolder = false }, enabled = !creatingFolder) {
                        Text(stringResource(R.string.action_cancel))
                    }
                TextButton(
                    enabled = folderName.isNotBlank() && !creatingFolder,
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
                                val newPath = File(currentPath, folderName.trim()).path
                                showCreateFolder = false
                                currentPath = newPath
                                resultMessage = null
                                error = null
                            }.onFailure {
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
}

private fun isInsideRoot(rootPath: String, candidatePath: String): Boolean = runCatching {
    val root = File(rootPath).canonicalPath.trimEnd(File.separatorChar)
    val candidate = File(candidatePath).canonicalPath
    candidate == root || candidate.startsWith(root + File.separator)
}.getOrDefault(false)
