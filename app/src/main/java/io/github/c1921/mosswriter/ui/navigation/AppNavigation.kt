package io.github.c1921.mosswriter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.c1921.mosswriter.MossWriterApp
import io.github.c1921.mosswriter.data.local.LocalFileRepository
import io.github.c1921.mosswriter.data.settings.SettingsRepository
import io.github.c1921.mosswriter.ui.editor.EditorScreen
import io.github.c1921.mosswriter.ui.filelist.FileListScreen
import io.github.c1921.mosswriter.ui.settings.SettingsScreen
import io.github.c1921.mosswriter.ui.setup.SetupScreen
import java.net.URLDecoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as MossWriterApp
    val localRepo = LocalFileRepository(context)
    val settingsRepo = SettingsRepository(app.encryptedPrefs)

    val startDestination = if (settingsRepo.hasProject()) Screen.FileList.route else Screen.Setup.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Setup.route) {
            SetupScreen(
                settingsRepo = settingsRepo,
                onProjectCreated = {
                    navController.navigate(Screen.FileList.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.FileList.route) {
            FileListScreen(
                localRepo = localRepo,
                settingsRepo = settingsRepo,
                onOpenFile = { fileName ->
                    navController.navigate(Screen.Editor.createRoute(fileName))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("fileName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("fileName") ?: return@composable
            val fileName = URLDecoder.decode(encodedName, "UTF-8")
            EditorScreen(
                fileName = fileName,
                localRepo = localRepo,
                settingsRepo = settingsRepo,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsRepo = settingsRepo,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
