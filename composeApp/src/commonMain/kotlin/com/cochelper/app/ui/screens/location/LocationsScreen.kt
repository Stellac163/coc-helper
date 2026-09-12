package com.cochelper.app.ui.screens.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.LocationEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.GroupedListItem
import com.cochelper.app.ui.components.SectionTopBar
import com.cochelper.app.ui.components.groupedCornerShape
import com.cochelper.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(moduleId: Long, navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val locations by container.locationDao.observeForModule(moduleId).collectAsState(emptyList())

    var showNew by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = "重要地点",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNew = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.Add, contentDescription = "新建地点") }
        },
    ) { padding ->
        if (locations.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                EmptyState("还没有地点，点击右下角新建", icon = Icons.Filled.LocationOn)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                locations.forEachIndexed { index, loc ->
                    GroupedListItem(
                        shape = groupedCornerShape(index, locations.size),
                        icon = Icons.Filled.LocationOn,
                        title = loc.name.ifBlank { "未命名地点" },
                        subtitle = loc.content.ifBlank { "暂无描述" },
                        trailing = {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { navController.navigate(Routes.location(loc.id)) },
                    )
                }
            }
        }
    }

    if (showNew) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("新建地点") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("地点名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch { container.locationDao.insert(LocationEntity(moduleId = moduleId, name = name.trim())) }
                        }
                        showNew = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("取消") } },
        )
    }
}
