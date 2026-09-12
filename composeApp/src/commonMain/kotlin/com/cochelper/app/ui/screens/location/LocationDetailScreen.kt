package com.cochelper.app.ui.screens.location

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.FloatingToolbar
import com.cochelper.app.ui.components.MarkdownText
import com.cochelper.app.ui.components.SectionTopBar
import com.cochelper.app.ui.components.ToolbarAction
import com.cochelper.app.ui.components.applyMarkup
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(locationId: Long, navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val location by container.locationDao.observeById(locationId).collectAsState(null)

    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var confirmDelete by remember { mutableStateOf(false) }

    val loc = location
    if (loc == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState("地点不存在或已被删除")
        }
        return
    }

    fun startEditing() {
        name = loc.name
        content = TextFieldValue(loc.content)
        editing = true
    }

    fun save() {
        scope.launch { container.locationDao.update(loc.copy(name = name.trim(), content = content.text)) }
    }

    val toolbarActions = listOf(
        ToolbarAction(Icons.Filled.FormatBold, "加粗") { content = applyMarkup(content, "**") },
        ToolbarAction(Icons.Filled.FormatItalic, "斜体") { content = applyMarkup(content, "*") },
        ToolbarAction(Icons.Filled.FormatUnderlined, "下划线") { content = applyMarkup(content, "__") },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = if (editing) "编辑地点" else "地点详情",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
                rightIcon = if (!editing) Icons.Filled.Delete else null,
                onRightClick = { confirmDelete = true },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 96.dp),
            ) {
                if (editing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("地点名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it; save() },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("地点描述…") },
                    )
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            loc.name.ifBlank { "未命名地点" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(16.dp))
                        if (loc.content.isBlank()) {
                            Text(
                                "还没有描述，点击右下角编辑",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            MarkdownText(loc.content)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (editing) {
                    FloatingToolbar(actions = toolbarActions)
                }
                Spacer(Modifier.weight(1f))
                FloatingActionButton(
                    onClick = {
                        if (editing) {
                            save()
                            editing = false
                        } else {
                            startEditing()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) { Icon(Icons.Filled.Edit, contentDescription = "编辑") }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("删除地点") },
            text = { Text("确定要删除“${loc.name}”吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        container.locationDao.delete(loc)
                        navController.navigateUp()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}
