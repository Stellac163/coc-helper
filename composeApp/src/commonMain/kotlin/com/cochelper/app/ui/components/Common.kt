package com.cochelper.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 图片占位符（surfaceContainerHighest 背景 + 图标）。 */
@Composable
fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Image,
    corner: Dp = 20.dp,
    iconSize: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 从 Uri / data URL 加载图片，失败或为空时显示占位符。 */
@Composable
fun LoadedImage(
    uri: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Image,
    corner: Dp = 20.dp,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (uri.isNullOrBlank()) {
        ImagePlaceholder(modifier = modifier, icon = icon, corner = corner)
    } else {
        PlatformImage(
            model = uri,
            modifier = modifier.clip(RoundedCornerShape(corner)),
            contentScale = contentScale,
        )
    }
}

/** 空状态提示。 */
@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Warning,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 顶部应用栏（左侧图标非返回箭头时使用）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leftIcon: ImageVector? = null,
    onLeftClick: (() -> Unit)? = null,
    rightIcon: ImageVector? = null,
    onRightClick: (() -> Unit)? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (leftIcon != null) {
                IconButton(onClick = { onLeftClick?.invoke() }) {
                    Icon(leftIcon, contentDescription = null)
                }
            }
        },
        actions = {
            if (rightIcon != null) {
                IconButton(onClick = { onRightClick?.invoke() }) {
                    Icon(rightIcon, contentDescription = null)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/** 圆形 primaryContainer 图标徽标（40dp，用于列表项 leading 图标）。 */
@Composable
fun CircleIconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** 无涟漪点击（用于文本类可点击元素，按规范不加涟漪）。 */
@Composable
fun Modifier.noRippleClick(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    )
}

/** 分组列表（M3 Expressive 连接列表）的角形状：外侧 28dp、相邻内侧 8dp。 */
fun groupedCornerShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 28.dp
    val inner = 8.dp
    val isFirst = index == 0
    val isLast = index == count - 1
    return when {
        count == 1 -> RoundedCornerShape(outer)
        isFirst -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomEnd = inner, bottomStart = inner)
        isLast -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomEnd = outer, bottomStart = outer)
        else -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomEnd = inner, bottomStart = inner)
    }
}
