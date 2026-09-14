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
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cochelper.app.di.AppContainer
import com.cochelper.app.generated.resources.Res
import com.cochelper.app.generated.resources.noto_sans_sc_regular
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.LocalIsDesktop
import com.cochelper.app.ui.components.SideBar
import com.cochelper.app.ui.components.navigateToTab
import com.cochelper.app.ui.navigation.AppNavHost
import com.cochelper.app.ui.navigation.BrowserBackHandler
import com.cochelper.app.ui.theme.CocHelperTheme
import com.cochelper.app.ui.theme.ThemeMode
import com.cochelper.app.platform.currentTimeMillis
import com.cochelper.app.platform.hideAppLoadingOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

/** 手机窄屏时限制为手机宽度居中显示。 */
private val MOBILE_MAX_WIDTH = 480.dp

/** 桌面宽屏：左侧边栏宽度 + 断点宽度 + 内容区最大宽度。 */
private val SIDEBAR_WIDTH = 240.dp
private val DESKTOP_BREAKPOINT = 720.dp
private val DESKTOP_CONTENT_MAX = 960.dp

/** 网页版根 Composable。 */
@OptIn(ExperimentalResourceApi::class, ExperimentalTextApi::class)
@Composable
fun App(container: AppContainer) {
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    val scope = rememberCoroutineScope()

    // 预加载中文字体：preloadFont 返回带字节的 Font（未就绪时为空），
    // 再把它真正解析成 Skia Typeface 并写进文本排版所用的同一个缓存，保证揭幕即渲染出字形（否则先显示空心方块）。
    // 用 LocalFontFamilyResolver.current（而非新建 resolver），确保预热命中文字实际解析时用的缓存。
    val appFont by preloadFont(Res.font.noto_sans_sc_regular)
    val fontResolver = LocalFontFamilyResolver.current
    val fontLoadStart = remember { currentTimeMillis() }
    LaunchedEffect(appFont) {
        val font = appFont
        if (font == null) {
            println("[font] +${currentTimeMillis() - fontLoadStart}ms 字节仍未就绪")
            return@LaunchedEffect
        }
        println("[font] +${currentTimeMillis() - fontLoadStart}ms 字节就绪，开始解析 typeface")
        val t1 = currentTimeMillis()
        try {
            fontResolver.preload(FontFamily(font))
            println("[font] +${currentTimeMillis() - fontLoadStart}ms typeface 解析完成（耗时 ${currentTimeMillis() - t1}ms）")
        } catch (t: Throwable) {
            println("[font] 预加载失败：${t.message}")
        }
        println("[font] +${currentTimeMillis() - fontLoadStart}ms 移除加载遮罩")
        hideAppLoadingOverlay()
    }
    // 兜底：字体加载异常/超时也不至于永久卡在加载页。
    LaunchedEffect(Unit) {
        delay(15_000)
        hideAppLoadingOverlay()
    }

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
        CocHelperTheme(themeMode = themeMode, fontFamily = appFont?.let { FontFamily(it) }) {
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
