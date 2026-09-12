package com.cochelper.app.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.cochelper.app.data.AppSettings
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.AppBottomBar
import com.cochelper.app.ui.components.ImagePlaceholder
import com.cochelper.app.ui.components.LoadedImage
import com.cochelper.app.ui.components.navigateToTab
import com.cochelper.app.ui.navigation.Routes
import com.cochelper.app.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    navController: NavHostController,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val container = LocalContainer.current
    val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            AppBottomBar(currentRoute = Routes.TOOLS) { route -> navController.navigateToTab(route) }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WelcomeHeader(
                nickname = settings.nickname,
                avatarUri = settings.avatarUri,
                onLogin = { navController.navigate(Routes.LOGIN) },
            )

            Column(Modifier.padding(horizontal = 16.dp)) {
                NightModeRow(
                    checked = settings.themeMode == ThemeMode.DARK,
                    onCheckedChange = { checked ->
                        onThemeModeChange(if (checked) ThemeMode.DARK else ThemeMode.SYSTEM)
                    },
                )
            }

            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolCard(
                        title = "战斗轮小助手",
                        onClick = { navController.navigate(Routes.COMBAT) },
                        modifier = Modifier.weight(1f),
                    )
                    ToolCard(
                        title = "追逐战小助手",
                        onClick = { navController.navigate(Routes.CHASE) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolCard(
                        title = "计时器",
                        onClick = { navController.navigate(Routes.TIMER) },
                        modifier = Modifier.weight(1f),
                    )
                    ToolCard(
                        title = "线索板",
                        onClick = { navController.navigate(Routes.CLUES) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader(
    nickname: String,
    avatarUri: String,
    onLogin: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(236.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avatarUri.isBlank()) {
                    ImagePlaceholder(
                        modifier = Modifier.size(148.dp),
                        icon = Icons.Filled.Person,
                        corner = 20.dp,
                        iconSize = 56.dp,
                    )
                } else {
                    LoadedImage(
                        uri = avatarUri,
                        modifier = Modifier.size(148.dp),
                        icon = Icons.Filled.Person,
                        corner = 20.dp,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "欢迎！",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                nickname.ifBlank { "调查员" },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SmallFloatingActionButton(
            onClick = onLogin,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) { Icon(Icons.Filled.Login, contentDescription = "登录") }
    }
}

@Composable
private fun NightModeRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                "夜间模式",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = modifier.height(208.dp),
    ) {
        Column {
            ImagePlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                corner = 20.dp,
                icon = Icons.Filled.Image,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
