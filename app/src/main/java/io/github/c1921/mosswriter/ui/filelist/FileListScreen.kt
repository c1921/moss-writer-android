package io.github.c1921.mosswriter.ui.filelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileError by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(settingsRepo.getProjectName().ifBlank { "Moss Writer" }) },
                    actions = {
                        IconButton(onClick = { vm.sync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "同步")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                )
                if (syncState is SyncState.Syncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newFileName = ""
                newFileError = false
                showNewFileDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "新建文件")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无文件，点击 + 新建", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(files, key = { it.name }) { file ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                vm.deleteFile(file.name)
                                true
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
                        FileItem(file = file, onClick = { onOpenFile(file.name) })
                    }
                }
            }
        }
    }

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
                        val fileName = if (newFileName.trim().endsWith(".md")) newFileName.trim()
                        else "${newFileName.trim()}.md"
                        onOpenFile(fileName)
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
}

@Composable
private fun FileItem(file: io.github.c1921.mosswriter.data.model.NoteFile, onClick: () -> Unit) {
    val displayName = file.name.removeSuffix(".md")
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
