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

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val projectName: String
        get() = settingsRepo.getProjectName()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _files.value = localRepo.listFiles(projectName)
        }
    }

    fun createFile(name: String): Boolean {
        val fileName = if (name.endsWith(".md")) name else "$name.md"
        if (localRepo.fileExists(projectName, fileName)) return false
        localRepo.writeFile(projectName, fileName, "")
        loadFiles()
        return true
    }

    fun deleteFile(name: String) {
        localRepo.deleteFile(projectName, name)
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
