package com.cochelper.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.ModuleEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.AppBottomBar
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.GroupedListItem
import com.cochelper.app.ui.components.ImagePlaceholder
import com.cochelper.app.ui.components.groupedCornerShape
import com.cochelper.app.ui.components.navigateToTab
import com.cochelper.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val modules by container.moduleDao.observeAll().collectAsStateWithLifecycle(emptyList())
    val activeModule = modules.firstOrNull { it.isActive }

    var showNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            AppBottomBar(currentRoute = Routes.HOME) { route -> navController.navigateToTab(route) }
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showNewDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.Edit, contentDescription = "新建模组") }
        },
    ) { padding ->
        if (modules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                EmptyState("还没有模组，点击右下角按钮新建一个模组", icon = Icons.Filled.Image)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                activeModule?.let { module ->
                    ActiveModuleCard(
                        module = module,
                        onClick = { navController.navigate(Routes.module(module.id, "slide")) },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    modules.forEachIndexed { index, module ->
                        GroupedListItem(
                            shape = groupedCornerShape(index, modules.size),
                            icon = Icons.Filled.Home,
                            title = module.name.ifBlank { "未命名模组" },
                            subtitle = module.description.ifBlank { "模组简介" },
                            trailing = {
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = { navController.navigate(Routes.module(module.id, "fade")) },
                        )
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewModuleDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { name, desc ->
                scope.launch {
                    container.moduleDao.insert(ModuleEntity(name = name, description = desc))
                }
                showNewDialog = false
            },
        )
    }
}

@Composable
private fun ActiveModuleCard(module: ModuleEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            ImagePlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                corner = 20.dp,
            )
            Column(Modifier.padding(20.dp)) {
                Text(module.name.ifBlank { "模组名称" }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    module.description.ifBlank { "模组简介" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NewModuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("新建模组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("模组名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("模组简介") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), desc.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
