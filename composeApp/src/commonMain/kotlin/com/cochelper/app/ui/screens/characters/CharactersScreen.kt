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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.PcEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.AppBottomBar
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.LoadedImage
import com.cochelper.app.ui.components.SkillCountBadge
import com.cochelper.app.ui.components.navigateToTab
import com.cochelper.app.ui.components.pcStatsSummary
import com.cochelper.app.ui.components.skilledCount
import com.cochelper.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val pcs by container.pcDao.observePlayerPcs().collectAsState(emptyList())
    val snackbar = remember { SnackbarHostState() }

    var showNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AppBottomBar(currentRoute = Routes.CHARACTERS) { route -> navController.navigateToTab(route) }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewDialog = true },
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
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "角色",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                scope.launch { container.pcDao.insert(PcEntity(name = name)) }
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
        Row(Modifier.height(150.dp)) {
            LoadedImage(
                uri = pc.imageUri.ifBlank { null },
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                icon = Icons.Filled.Person,
                corner = 0.dp,
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(pc.name.ifBlank { "人物名称" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    pcSummary(pc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pcStatsSummary(pc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    SkillCountBadge(count = skilledCount(pc))
                }
            }
        }
    }
}

@Composable
private fun NewPcDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("新建角色") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("人物名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
