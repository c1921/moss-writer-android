package io.github.c1921.mosswriter.data.settings

import android.content.SharedPreferences
import io.github.c1921.mosswriter.data.model.WebDavConfig

class SettingsRepository(private val prefs: SharedPreferences) {

    fun getWebDavConfig(): WebDavConfig = WebDavConfig(
        url = prefs.getString(KEY_URL, "") ?: "",
        username = prefs.getString(KEY_USERNAME, "") ?: "",
        password = prefs.getString(KEY_PASSWORD, "") ?: "",
        projectName = prefs.getString(KEY_PROJECT_NAME, "") ?: ""
    )

    fun saveWebDavConfig(config: WebDavConfig) {
        prefs.edit()
            .putString(KEY_URL, config.url)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .putString(KEY_PROJECT_NAME, config.projectName)
            .apply()
    }

    fun getProjectName(): String = prefs.getString(KEY_PROJECT_NAME, "") ?: ""

    fun saveProjectName(name: String) {
        prefs.edit().putString(KEY_PROJECT_NAME, name).apply()
    }

    fun hasProject(): Boolean = getProjectName().isNotBlank()

    companion object {
        private const val KEY_URL = "webdav_url"
        private const val KEY_USERNAME = "webdav_username"
        private const val KEY_PASSWORD = "webdav_password"
        private const val KEY_PROJECT_NAME = "project_name"
    }
}
