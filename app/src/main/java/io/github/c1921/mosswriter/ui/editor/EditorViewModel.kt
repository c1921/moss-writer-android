package io.github.c1921.mosswriter.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.c1921.mosswriter.data.local.LocalFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditorViewModel(
    private val localRepo: LocalFileRepository
) : ViewModel() {

    var projectName: String = ""
        private set
    var fileName: String = ""
        private set

    var content by mutableStateOf("")
    var isDirty by mutableStateOf(false)

    fun load(project: String, name: String) {
        projectName = project
        fileName = name
        viewModelScope.launch(Dispatchers.IO) {
            val text = localRepo.readFile(project, name)
            content = text
            isDirty = false
        }
    }

    fun onContentChange(newContent: String) {
        content = newContent
        isDirty = true
    }

    fun save() {
        if (!isDirty) return
        viewModelScope.launch(Dispatchers.IO) {
            localRepo.writeFile(projectName, fileName, content)
            isDirty = false
        }
    }
}
