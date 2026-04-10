package io.github.c1921.mosswriter.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object FileList : Screen("file_list")
    object Editor : Screen("editor/{fileName}") {
        fun createRoute(fileName: String) = "editor/${java.net.URLEncoder.encode(fileName, "UTF-8")}"
    }
    object Settings : Screen("settings")
}
