package io.github.c1921.mosswriter.ui.filelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.c1921.mosswriter.data.local.LocalFileRepository
import io.github.c1921.mosswriter.data.model.NoteFile
import io.github.c1921.mosswriter.data.remote.WebDavRepository
import io.github.c1921.mosswriter.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

class FileListViewModel(
    private val localRepo: LocalFileRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _files = MutableStateFlow<List<NoteFile>>(emptyList())
    val files: StateFlow<List<NoteFile>> = _files

    private val _currentPath = MutableStateFlow<List<String>>(emptyList())
    val currentPath: StateFlow<List<String>> = _currentPath

    val canNavigateUp: Boolean get() = _currentPath.value.isNotEmpty()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val projectName: String
        get() = settingsRepo.getProjectName()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _files.value = localRepo.listEntries(projectName, _currentPath.value)
        }
    }

    fun navigateInto(folderName: String) {
        _currentPath.value = _currentPath.value + folderName
        loadFiles()
    }

    fun navigateUp() {
        if (_currentPath.value.isEmpty()) return
        _currentPath.value = _currentPath.value.dropLast(1)
        loadFiles()
    }

    fun navigateToIndex(index: Int) {
        _currentPath.value = _currentPath.value.take(index + 1)
        loadFiles()
    }

    fun fullFileRelativePath(name: String): String =
        (_currentPath.value + name).joinToString("/")

    fun createFile(name: String): Boolean {
        val fileName = if (name.endsWith(".md")) name else "$name.md"
        if (localRepo.fileExists(projectName, _currentPath.value, fileName)) return false
        localRepo.writeFile(projectName, _currentPath.value, fileName, "")
        loadFiles()
        return true
    }

    fun createFolder(name: String): Boolean {
        if (name.isBlank()) return false
        val created = localRepo.createFolder(projectName, _currentPath.value, name)
        if (created) loadFiles()
        return created
    }

    fun deleteFile(name: String) {
        localRepo.deleteFile(projectName, _currentPath.value, name)
        loadFiles()
    }

    fun deleteFolder(name: String) {
        localRepo.deleteFolder(projectName, _currentPath.value + name)
        loadFiles()
    }

    fun sync() {
        val config = settingsRepo.getWebDavConfig()
        if (!config.isConfigured) {
            _syncState.value = SyncState.Error("请先在设置中配置 WebDAV 服务器和项目名称")
            return
        }
        _syncState.value = SyncState.Syncing
        viewModelScope.launch(Dispatchers.IO) {
            val result = WebDavRepository(localRepo, config).sync()
            _syncState.value = result.fold(
                onSuccess = { SyncState.Success("同步完成") },
                onFailure = { SyncState.Error(it.message ?: "同步失败") }
            )
            loadFiles()
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }
}
