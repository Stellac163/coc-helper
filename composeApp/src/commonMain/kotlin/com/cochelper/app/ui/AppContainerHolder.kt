package com.cochelper.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.cochelper.app.di.AppContainer

/** 提供手动 DI 容器给整个 Compose 树。 */
val LocalContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer 未提供")
}
