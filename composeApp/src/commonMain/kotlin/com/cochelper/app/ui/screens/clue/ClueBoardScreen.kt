package com.cochelper.app.ui.screens.clue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.ClueEntity
import com.cochelper.app.di.AppContainer
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.SectionTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClueBoardScreen(navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val clues by container.clueDao.observeAll().collectAsState(emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ClueEntity?>(null) }
    var deleting by remember { mutableStateOf<ClueEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = "线索板",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.Add, contentDescription = "新建线索") }
        },
    ) { padding ->
        if (clues.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                EmptyState("还没有线索，点击右下角添加", icon = Icons.Filled.Lightbulb)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                clues.forEachIndexed { index, clue ->
                    ClueCard(
                        clue = clue,
                        onEdit = { editing = clue },
                        onDelete = { deleting = clue },
                        onMoveUp = { scope.launch { moveClue(container, clues, index, -1) } },
                        onMoveDown = { scope.launch { moveClue(container, clues, index, 1) } },
                        canMoveUp = index > 0,
                        canMoveDown = index < clues.size - 1,
                    )
                }
            }
        }
    }

    if (showAdd || editing != null) {
        val target = editing
        var title by remember { mutableStateOf(target?.title ?: "") }
        var content by remember { mutableStateOf(target?.content ?: "") }
        AlertDialog(
            onDismissRequest = { showAdd = false; editing = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (target == null) "新建线索" else "编辑线索") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("内容") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            scope.launch {
                                if (target == null) {
                                    container.clueDao.insert(ClueEntity(title = title.trim(), content = content, order = clues.size))
                                } else {
                                    container.clueDao.update(target.copy(title = title.trim(), content = content))
                                }
                            }
                        }
                        showAdd = false
                        editing = null
                    },
                    enabled = title.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; editing = null }) { Text("取消") } },
        )
    }

    deleting?.let { clue ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("删除线索") },
            text = { Text("确定要删除“${clue.title}”吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.clueDao.delete(clue) }
                    deleting = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ClueCard(
    clue: ClueEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    clue.title.ifBlank { "未命名线索" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (clue.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        clue.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "上移",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.outline,
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "下移",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.outline,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private suspend fun moveClue(container: AppContainer, all: List<ClueEntity>, index: Int, delta: Int) {
    val sorted = all.sortedBy { it.order }.toMutableList()
    val target = index + delta
    if (index !in sorted.indices || target !in sorted.indices) return
    val moved = sorted.removeAt(index)
    sorted.add(target, moved)
    sorted.forEachIndexed { idx, c ->
        if (c.order != idx) container.clueDao.update(c.copy(order = idx))
    }
}
