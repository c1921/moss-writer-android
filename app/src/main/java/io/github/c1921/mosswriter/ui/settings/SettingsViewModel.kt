package io.github.c1921.mosswriter.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.c1921.mosswriter.data.model.WebDavConfig
import io.github.c1921.mosswriter.data.remote.WebDavClient
import io.github.c1921.mosswriter.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    var url by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var projectName by mutableStateOf("")
    var testResult by mutableStateOf<String?>(null)
    var isTesting by mutableStateOf(false)

    init {
        load()
    }

    fun load() {
        val config = settingsRepo.getWebDavConfig()
        url = config.url
        username = config.username
        password = config.password
        projectName = config.projectName
    }

    fun save() {
        settingsRepo.saveWebDavConfig(
            WebDavConfig(url.trim(), username.trim(), password, projectName.trim())
        )
    }

    fun testConnection() {
        val config = WebDavConfig(url.trim(), username.trim(), password, projectName.trim())
        if (!config.isConfigured) {
            testResult = "请填写服务器地址、用户名和项目名称"
            return
        }
        isTesting = true
        testResult = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = WebDavClient(config).testConnection()
            testResult = result.fold(
                onSuccess = { "连接成功，目录已就绪" },
                onFailure = { "连接失败: ${it.message}" }
            )
            isTesting = false
        }
    }

    fun clearTestResult() {
        testResult = null
    }
}
