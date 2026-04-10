package io.github.c1921.mosswriter.ui.filelist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.c1921.mosswriter.data.local.LocalFileRepository
import io.github.c1921.mosswriter.data.model.NoteFile
import io.github.c1921.mosswriter.data.settings.SettingsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    localRepo: LocalFileRepository,
    settingsRepo: SettingsRepository,
    onOpenFile: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val vm: FileListViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FileListViewModel(localRepo, settingsRepo) as T
            }
        }
    )

    val files by vm.files.collectAsState()
    val syncState by vm.syncState.collectAsState()
    val currentPath by vm.currentPath.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileError by remember { mutableStateOf(false) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newFolderError by remember { mutableStateOf(false) }

    var showFabMenu by remember { mutableStateOf(false) }
    var pendingDeleteFolder by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = vm.canNavigateUp) {
        vm.navigateUp()
    }

    LaunchedEffect(syncState) {
        when (val s = syncState) {
            is SyncState.Success -> {
                scope.launch { snackbarHostState.showSnackbar(s.message) }
                vm.clearSyncState()
            }
            is SyncState.Error -> {
                scope.launch { snackbarHostState.showSnackbar(s.message) }
                vm.clearSyncState()
            }
            else -> {}
        }
    }

    val projectName = settingsRepo.getProjectName().ifBlank { "Moss Writer" }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(projectName) },
                    actions = {
                        IconButton(onClick = { vm.sync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "同步")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                )
                BreadcrumbBar(
                    projectName = projectName,
                    currentPath = currentPath,
                    onNavigateToRoot = { vm.navigateToIndex(-1) },
                    onNavigateToIndex = { vm.navigateToIndex(it) }
                )
                if (syncState is SyncState.Syncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = showFabMenu) {
                    Column(horizontalAlignment = Alignment.End) {
                        SmallFloatingActionButton(onClick = {
                            showFabMenu = false
                            newFolderName = ""
                            newFolderError = false
                            showNewFolderDialog = true
                        }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "新建文件夹")
                        }
                        Spacer(Modifier.height(8.dp))
                        SmallFloatingActionButton(onClick = {
                            showFabMenu = false
                            newFileName = ""
                            newFileError = false
                            showNewFileDialog = true
                        }) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "新建文件")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                    Icon(
                        if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "操作"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (currentPath.isEmpty()) "暂无文件，点击 + 新建" else "此文件夹为空，点击 + 新建",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(files, key = { it.name }) { entry ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                if (entry.isDirectory) {
                                    pendingDeleteFolder = entry.name
                                    false
                                } else {
                                    vm.deleteFile(entry.name)
                                    true
                                }
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    ) {
                        FileEntryItem(
                            entry = entry,
                            onClick = {
                                if (entry.isDirectory) vm.navigateInto(entry.name)
                                else onOpenFile(vm.fullFileRelativePath(entry.name))
                            }
                        )
                    }
                }
            }
        }
    }

    // 新建文件对话框
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("新建文件") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = {
                        newFileName = it
                        newFileError = false
                    },
                    label = { Text("文件名") },
                    suffix = { Text(".md") },
                    isError = newFileError,
                    supportingText = if (newFileError) { { Text("文件名已存在或为空") } } else null,
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFileName.isBlank()) {
                        newFileError = true
                        return@TextButton
                    }
                    val created = vm.createFile(newFileName.trim())
                    if (created) {
                        showNewFileDialog = false
                        onOpenFile(vm.fullFileRelativePath(
                            if (newFileName.trim().endsWith(".md")) newFileName.trim()
                            else "${newFileName.trim()}.md"
                        ))
                    } else {
                        newFileError = true
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("取消") }
            }
        )
    }

    // 新建文件夹对话框
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = {
                        newFolderName = it
                        newFolderError = false
                    },
                    label = { Text("文件夹名") },
                    isError = newFolderError,
                    supportingText = if (newFolderError) { { Text("名称已存在或为空") } } else null,
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isBlank()) {
                        newFolderError = true
                        return@TextButton
                    }
                    val created = vm.createFolder(newFolderName.trim())
                    if (created) showNewFolderDialog = false else newFolderError = true
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("取消") }
            }
        )
    }

    // 文件夹删除确认对话框
    pendingDeleteFolder?.let { folderName ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFolder = null },
            title = { Text("删除文件夹") },
            text = { Text("确定要删除「$folderName」及其所有内容吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteFolder(folderName)
                    pendingDeleteFolder = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFolder = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun BreadcrumbBar(
    projectName: String,
    currentPath: List<String>,
    onNavigateToRoot: () -> Unit,
    onNavigateToIndex: (Int) -> Unit
) {
    if (currentPath.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            TextButton(onClick = onNavigateToRoot) {
                Text(projectName, style = MaterialTheme.typography.labelMedium)
            }
        }
        itemsIndexed(currentPath) { index, segment ->
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { onNavigateToIndex(index) }) {
                Text(segment, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun FileEntryItem(entry: NoteFile, onClick: () -> Unit) {
    val icon = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description
    val displayName = if (entry.isDirectory) entry.name else entry.name.removeSuffix(".md")
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.lastModified))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
