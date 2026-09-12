package com.cochelper.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.cochelper.app.CocHelperApp
import com.cochelper.app.ui.navigation.AppNavHost
import com.cochelper.app.ui.theme.CocHelperTheme
import com.cochelper.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CocHelperApp
        var themeMode by mutableStateOf(ThemeMode.SYSTEM)

        lifecycleScope.launch {
            themeMode = app.container.settings.settings.first().themeMode
        }

        setContent {
            val container = remember { app.container }
            val navController = rememberNavController()
            CompositionLocalProvider(LocalContainer provides container) {
                CocHelperTheme(themeMode = themeMode) {
                    AppNavHost(
                        navController = navController,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            lifecycleScope.launch { container.settings.setThemeMode(mode) }
                        },
                    )
                }
            }
        }
    }
}
