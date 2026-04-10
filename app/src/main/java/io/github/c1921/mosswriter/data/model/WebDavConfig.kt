package io.github.c1921.mosswriter.data.model

data class WebDavConfig(
    val url: String,
    val username: String,
    val password: String,
    val projectName: String = ""
) {
    val isConfigured: Boolean get() = url.isNotBlank() && username.isNotBlank() && projectName.isNotBlank()
    val projectUrl: String get() = url.trimEnd('/') + "/MossWriter/" + projectName.trimEnd('/') + "/"
}
