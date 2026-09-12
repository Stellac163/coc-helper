package com.cochelper.app.ui.screens.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.TimelineNodeEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.SectionTopBar
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val MAX_DEPTH = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(moduleId: Long, navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val nodes by container.timelineDao.observeForModule(moduleId).collectAsState(emptyList())

    var editing by remember { mutableStateOf<TimelineNodeEntity?>(null) }
    var addingUnder by remember { mutableStateOf<Long?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = "时间轴与大纲",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.Add, contentDescription = "新建时间轴") }
        },
    ) { padding ->
        if (nodes.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                EmptyState("还没有时间轴节点，点击右下角新建", icon = Icons.Filled.ReceiptLong)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
            ) {
                TimelineSiblingList(
                    nodes = nodes,
                    parentId = null,
                    depth = 0,
                    onEdit = { editing = it },
                    onAddChild = { parent -> addingUnder = parent.id },
                    onDelete = { node ->
                        scope.launch {
                            // 删除该节点及其所有后代
                            deleteSubtree(container, node, nodes)
                        }
                    },
                    onReorder = { id, from, to ->
                        scope.launch { reorderSiblings(container, nodes, id, from, to) }
                    },
                )
            }
        }
    }

    if (editing != null) {
        val node = editing!!
        var title by remember(node.id) { mutableStateOf(node.title) }
        var content by remember(node.id) { mutableStateOf(node.content) }
        AlertDialog(
            onDismissRequest = { editing = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("编辑节点") },
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
                TextButton(onClick = {
                    scope.launch { container.timelineDao.update(node.copy(title = title.trim(), content = content)) }
                    editing = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } },
        )
    }

    if (showAdd || addingUnder != null) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false; addingUnder = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (addingUnder == null) "新建事件" else "新建子事件") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        val siblings = nodes.filter { it.parentId == addingUnder }
                        scope.launch {
                            container.timelineDao.insert(
                                TimelineNodeEntity(
                                    moduleId = moduleId,
                                    title = title.trim(),
                                    parentId = addingUnder,
                                    order = siblings.size,
                                )
                            )
                        }
                    }
                    showAdd = false
                    addingUnder = null
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false; addingUnder = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TimelineSiblingList(
    nodes: List<TimelineNodeEntity>,
    parentId: Long?,
    depth: Int,
    onEdit: (TimelineNodeEntity) -> Unit,
    onAddChild: (TimelineNodeEntity) -> Unit,
    onDelete: (TimelineNodeEntity) -> Unit,
    onReorder: (id: Long, from: Int, to: Int) -> Unit,
) {
    val siblings = nodes.filter { it.parentId == parentId }.sortedBy { it.order }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        siblings.forEachIndexed { index, node ->
            TimelineNodeRow(
                node = node,
                depth = depth,
                nodes = nodes,
                isDragging = dragId == node.id,
                dragOffset = if (dragId == node.id) dragOffset else 0f,
                hasChildren = nodes.any { it.parentId == node.id },
                onDragStart = { dragId = node.id; dragOffset = 0f },
                onDrag = { delta -> dragOffset += delta },
                onDragEnd = {
                    val moved = (dragOffset / 56f).roundToInt()
                    if (moved != 0) {
                        onReorder(node.id, index, (index + moved).coerceIn(0, siblings.size - 1))
                    }
                    dragId = null
                    dragOffset = 0f
                },
                onEdit = onEdit,
                onAddChild = onAddChild,
                onDelete = onDelete,
                onReorder = onReorder,
            )
        }
    }
}

@Composable
private fun TimelineNodeRow(
    node: TimelineNodeEntity,
    depth: Int,
    nodes: List<TimelineNodeEntity>,
    isDragging: Boolean,
    dragOffset: Float,
    hasChildren: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onEdit: (TimelineNodeEntity) -> Unit,
    onAddChild: (TimelineNodeEntity) -> Unit,
    onDelete: (TimelineNodeEntity) -> Unit,
    onReorder: (id: Long, from: Int, to: Int) -> Unit,
) {
    var expanded by remember(node.id) { mutableStateOf(false) }

    Column {
        // 幕布分层：不同层级用不同底色 + 左侧色条，越深层越像叠上去的一层幕布
        val bg = when (depth % 3) {
            0 -> MaterialTheme.colorScheme.surfaceContainerHigh
            1 -> MaterialTheme.colorScheme.surfaceContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
        val accent = when (depth % 3) {
            0 -> MaterialTheme.colorScheme.primary
            1 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.secondary
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, dragOffset.roundToInt()) },
            shape = RoundedCornerShape(12.dp),
            color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else bg,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accent),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = "拖拽排序",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .pointerInput(node.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart() },
                                    onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() },
                                )
                            },
                    )
                    if (hasChildren) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (expanded) "收起" else "展开",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { expanded = !expanded },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            node.title.ifBlank { "未命名" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { onEdit(node) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (depth < MAX_DEPTH - 1) {
                        IconButton(onClick = { onAddChild(node) }) {
                            Icon(Icons.Filled.Add, contentDescription = "添加子事件", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { onDelete(node) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
                if (expanded && node.content.isNotBlank()) {
                    Text(
                        node.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (16 + depth * 20).dp, end = 16.dp, bottom = 14.dp),
                    )
                }
            }
        }
        if (expanded && hasChildren) {
            Box(
                Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth(),
            ) {
                TimelineSiblingList(
                    nodes = nodes,
                    parentId = node.id,
                    depth = depth + 1,
                    onEdit = onEdit,
                    onAddChild = onAddChild,
                    onDelete = onDelete,
                    onReorder = onReorder,
                )
            }
        }
    }
}

private suspend fun deleteSubtree(
    container: com.cochelper.app.di.AppContainer,
    node: TimelineNodeEntity,
    all: List<TimelineNodeEntity>,
) {
    val toDelete = mutableListOf(node.id)
    var changed = true
    while (changed) {
        changed = false
        all.filter { it.parentId in toDelete && it.id !in toDelete }.forEach {
            toDelete.add(it.id)
            changed = true
        }
    }
    all.filter { it.id in toDelete }.forEach { container.timelineDao.delete(it) }
}

private suspend fun reorderSiblings(
    container: com.cochelper.app.di.AppContainer,
    all: List<TimelineNodeEntity>,
    id: Long,
    from: Int,
    to: Int,
) {
    val node = all.firstOrNull { it.id == id } ?: return
    val siblings = all.filter { it.parentId == node.parentId }.sortedBy { it.order }.toMutableList()
    if (from == to || from !in siblings.indices || to !in siblings.indices) return
    val moved = siblings.removeAt(from)
    siblings.add(to, moved)
    siblings.forEachIndexed { idx, n ->
        if (n.order != idx) container.timelineDao.update(n.copy(order = idx))
    }
}
