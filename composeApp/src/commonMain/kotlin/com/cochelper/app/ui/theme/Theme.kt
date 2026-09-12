package com.cochelper.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.cochelper.app.generated.resources.Res
import com.cochelper.app.generated.resources.noto_sans_sc_regular
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.Font

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * 应用默认字体族。网页版（wasmJs）用 CanvasKit 渲染，不含系统字体，
 * 必须显式打包中文字体，否则汉字会显示成空心方块（tofu）。
 */
@Composable
private fun appFontFamily(): FontFamily = FontFamily(Font(Res.font.noto_sans_sc_regular))

private fun TextStyle.withAppFont(fontFamily: FontFamily): TextStyle =
    copy(fontFamily = fontFamily)

/** 基于 Material3 默认排版，把所有字重/字号样式统一替换为打包的中文字体。 */
@Composable
private fun appTypography(): Typography {
    val fontFamily = appFontFamily()
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.withAppFont(fontFamily),
        displayMedium = base.displayMedium.withAppFont(fontFamily),
        displaySmall = base.displaySmall.withAppFont(fontFamily),
        headlineLarge = base.headlineLarge.withAppFont(fontFamily),
        headlineMedium = base.headlineMedium.withAppFont(fontFamily),
        headlineSmall = base.headlineSmall.withAppFont(fontFamily),
        titleLarge = base.titleLarge.withAppFont(fontFamily),
        titleMedium = base.titleMedium.withAppFont(fontFamily),
        titleSmall = base.titleSmall.withAppFont(fontFamily),
        bodyLarge = base.bodyLarge.withAppFont(fontFamily),
        bodyMedium = base.bodyMedium.withAppFont(fontFamily),
        bodySmall = base.bodySmall.withAppFont(fontFamily),
        labelLarge = base.labelLarge.withAppFont(fontFamily),
        labelMedium = base.labelMedium.withAppFont(fontFamily),
        labelSmall = base.labelSmall.withAppFont(fontFamily),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CocHelperTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        content = content,
    )
}
