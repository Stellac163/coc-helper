package com.cochelper.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cochelper.app.di.AppContainer
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.LocalIsDesktop
import com.cochelper.app.ui.components.SideBar
import com.cochelper.app.ui.components.navigateToTab
import com.cochelper.app.ui.navigation.AppNavHost
import com.cochelper.app.ui.navigation.BrowserBackHandler
import com.cochelper.app.ui.theme.CocHelperTheme
import com.cochelper.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 手机窄屏时限制为手机宽度居中显示。 */
private val MOBILE_MAX_WIDTH = 480.dp

/** 桌面宽屏：左侧边栏宽度 + 断点宽度 + 内容区最大宽度。 */
private val SIDEBAR_WIDTH = 240.dp
private val DESKTOP_BREAKPOINT = 720.dp
private val DESKTOP_CONTENT_MAX = 960.dp

/** 网页版根 Composable。 */
@Composable
fun App(container: AppContainer) {
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        themeMode = container.settings.settings.first().themeMode
    }

    val navController = rememberNavController()
    BrowserBackHandler(navController)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val onThemeModeChange: (ThemeMode) -> Unit = { mode ->
        themeMode = mode
        scope.launch { container.settings.setThemeMode(mode) }
    }

    CompositionLocalProvider(LocalContainer provides container) {
        CocHelperTheme(themeMode = themeMode) {
            // 外圈衬底：桌面用「左侧边栏 + 右侧内容」的 dashboard 布局，窄屏用居中手机宽度。
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                val desktop = maxWidth >= DESKTOP_BREAKPOINT

                CompositionLocalProvider(LocalIsDesktop provides desktop) {
                    if (desktop) {
                        Row(Modifier.fillMaxSize()) {
                            SideBar(
                                currentRoute = currentRoute,
                                onNavigate = { navController.navigateToTab(it) },
                                modifier = Modifier
                                    .width(SIDEBAR_WIDTH)
                                    .fillMaxHeight(),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .widthIn(max = DESKTOP_CONTENT_MAX)
                                        .fillMaxWidth(),
                                ) {
                                    AppNavHost(navController = navController, onThemeModeChange = onThemeModeChange)
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = MOBILE_MAX_WIDTH)
                                    .fillMaxWidth(),
                            ) {
                                AppNavHost(navController = navController, onThemeModeChange = onThemeModeChange)
                            }
                        }
                    }
                }
            }
        }
    }
}
