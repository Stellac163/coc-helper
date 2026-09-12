package com.cochelper.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/** 平台图片加载：支持 data: 与 http(s) 地址，解码失败时什么都不渲染。 */
@Composable
expect fun PlatformImage(
    model: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
)
