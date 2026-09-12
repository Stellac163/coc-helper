package com.cochelper.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * 网页版实现：前进导航时向浏览器历史 push 一条记录；用户按返回键触发 popstate 时，
 * 在 App 内 pop 一层。已在根页面时不拦截，让浏览器正常退出。
 */
@Composable
actual fun BrowserBackHandler(navController: NavHostController) {
    DisposableEffect(navController) {
        // 由 popstate 触发的返回不该再 push 历史，用这个标志跳过一次
        var suppressPush = false

        val onDestination = NavController.OnDestinationChangedListener { _, _, _ ->
            if (suppressPush) {
                suppressPush = false
            } else {
                window.history.pushState(null, "")
            }
        }
        navController.addOnDestinationChangedListener(onDestination)

        val onPopState: (Event) -> Unit = {
            if (navController.previousBackStackEntry != null) {
                suppressPush = true
                navController.popBackStack()
            }
        }
        window.addEventListener("popstate", onPopState)

        onDispose {
            window.removeEventListener("popstate", onPopState)
            navController.removeOnDestinationChangedListener(onDestination)
        }
    }
}
