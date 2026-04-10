package io.github.c1921.mosswriter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.c1921.mosswriter.ui.navigation.AppNavigation
import io.github.c1921.mosswriter.ui.theme.MossWriterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MossWriterTheme {
                AppNavigation()
            }
        }
    }
}
