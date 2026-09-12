package com.cochelper.app.ui.screens.module

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.ModuleEntity
import com.cochelper.app.platform.compressImageDataUrl
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.GroupedListItem
import com.cochelper.app.ui.components.LoadedImage
import com.cochelper.app.ui.components.SectionTopBar
import com.cochelper.app.ui.components.groupedCornerShape
import com.cochelper.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(moduleId: Long, navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val module by container.moduleDao.observeById(moduleId).collectAsState(null)

    var confirmDelete by remember { mutableStateOf(false) }

    if (module == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState("模组不存在或已被删除")
        }
        return
    }

    val m = module!!
    val sections = listOf(
        Triple(Icons.Filled.Chat, "简介与招募") { navController.navigate(Routes.intro(moduleId)) },
        Triple(Icons.Filled.ReceiptLong, "时间轴与大纲") { navController.navigate(Routes.timeline(moduleId)) },
        Triple(Icons.Filled.LocationOn, "重要地点") { navController.navigate(Routes.locations(moduleId)) },
        Triple(Icons.Filled.Groups, "重要npc") { navController.navigate(Routes.npcs(moduleId)) },
        Triple(Icons.Filled.Person, "pc档案") { navController.navigate(Routes.pcs(moduleId)) },
    )

    fun setPhoto() {
        scope.launch {
            val picked = container.files.pickFile(arrayOf("image/*"))
            if (picked != null) {
                val compressed = compressImageDataUrl(picked.dataUrl, 960, 0.82)
                container.moduleDao.getById(moduleId)?.let {
                    container.moduleDao.update(it.copy(photoUri = compressed))
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = m.name.ifBlank { "模组详情" },
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
                rightIcon = Icons.Filled.Delete,
                onRightClick = { confirmDelete = true },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OngoingSwitch(
                checked = m.isActive,
                onCheckedChange = { checked ->
                    scope.launch {
                        if (checked) {
                            container.moduleDao.clearActive()
                            container.moduleDao.setActive(moduleId)
                        } else {
                            container.moduleDao.clearActive()
                        }
                    }
                },
            )

            ModuleBanner(
                module = m,
                onOpenOriginal = { navController.navigate(Routes.original(moduleId)) },
                onSetPhoto = { setPhoto() },
            )

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                sections.forEachIndexed { index, (icon, title, action) ->
                    GroupedListItem(
                        shape = groupedCornerShape(index, sections.size),
                        icon = icon,
                        title = title,
                        trailing = {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { action() },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("删除模组") },
            text = { Text("确定要删除“${m.name}”吗？模组下的时间轴、地点、npc、pc档案与文件都会被一并删除，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.deleteModule(moduleId)
                        navController.navigateUp()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun OngoingSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "正在进行",
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
private fun ModuleBanner(module: ModuleEntity, onOpenOriginal: () -> Unit, onSetPhoto: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.height(220.dp)) {
            LoadedImage(
                uri = module.photoUri.ifBlank { null },
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Filled.Image,
                corner = 20.dp,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
            )
            Text(
                module.name.ifBlank { "模组名称" },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp),
            )
            IconButton(
                onClick = onSetPhoto,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    Icons.Filled.AddAPhoto,
                    contentDescription = "设置照片",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Button(
                onClick = onOpenOriginal,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("原文")
            }
        }
    }
}
