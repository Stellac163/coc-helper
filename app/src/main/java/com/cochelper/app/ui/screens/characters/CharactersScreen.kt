package com.cochelper.app.ui.screens.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.cochelper.app.data.local.PcEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.AppBottomBar
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.LoadedImage
import com.cochelper.app.ui.components.navigateToTab
import com.cochelper.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val pcs by container.pcDao.observeAll().collectAsStateWithLifecycle(emptyList())
    val modules by container.moduleDao.observeAll().collectAsStateWithLifecycle(emptyList())
    val snackbar = remember { SnackbarHostState() }

    var showNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AppBottomBar(currentRoute = Routes.CHARACTERS) { route -> navController.navigateToTab(route) }
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = {
                    if (modules.isEmpty()) {
                        scope.launch { snackbar.showSnackbar("请先在“模组”页创建模组") }
                    } else {
                        showNewDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.Edit, contentDescription = "新建角色") }
        },
    ) { padding ->
        if (pcs.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                EmptyState("还没有角色，点击右下角新建调查员", icon = Icons.Filled.Person)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                pcs.forEach { pc ->
                    PcCard(
                        pc = pc,
                        onClick = { navController.navigate(Routes.pc(pc.id, avatar = true)) },
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        NewPcDialog(
            modules = modules,
            onDismiss = { showNewDialog = false },
            onConfirm = { name, moduleId ->
                scope.launch {
                    container.pcDao.insert(PcEntity(moduleId = moduleId, name = name))
                }
                showNewDialog = false
            },
        )
    }
}

private fun pcSummary(pc: PcEntity): String {
    val parts = listOfNotNull(
        pc.player.takeIf { it.isNotBlank() }?.let { "玩家 $it" },
        pc.gender.takeIf { it.isNotBlank() },
        pc.age.takeIf { it.isNotBlank() }?.let { "$it 岁" },
    )
    return if (parts.isEmpty()) "人物基础属性及简介" else parts.joinToString(" · ")
}

@Composable
private fun PcCard(pc: PcEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(140.dp)) {
            LoadedImage(
                uri = pc.imageUri.ifBlank { null },
                modifier = Modifier
                    .width(136.dp)
                    .fillMaxHeight(),
                icon = Icons.Filled.Person,
                corner = 20.dp,
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(20.dp),
            ) {
                Text(pc.name.ifBlank { "人物名称" }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    pcSummary(pc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPcDialog(
    modules: List<ModuleEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, moduleId: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedModule by remember { mutableStateOf(modules.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("新建角色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("人物名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedModule?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("所属模组") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        modules.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.name) },
                                onClick = {
                                    selectedModule = m
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mid = selectedModule?.id ?: modules.first().id
                    if (name.isNotBlank()) onConfirm(name.trim(), mid)
                },
                enabled = name.isNotBlank() && modules.isNotEmpty(),
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
