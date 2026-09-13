package com.cochelper.app.ui.screens.chase

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.ChaseParticipantEntity
import com.cochelper.app.data.local.ChasePointEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.SectionTopBar
import kotlinx.coroutines.launch

private fun roleLabel(role: Int) = if (role == ChaseParticipantEntity.CHASE_PURSUER) "追捕者" else "逃亡者"

/** 某参与者的本轮行动点：移动力最低者 1 次，每高出最低 MOV 1 点多 1 次。 */
private fun actionPoints(mov: Int, minMov: Int) = 1 + (mov - minMov)

/** 点位名（未分配显示「—」）。 */
private fun pointName(points: List<ChasePointEntity>, pointId: Long): String =
    points.firstOrNull { it.id == pointId }?.name ?: "—"

/** 沿有序点位把参与者移动 [delta] 个位置（越界不动；未分配时前进到首位、后退到末位）。 */
private fun moveParticipant(
    points: List<ChasePointEntity>,
    p: ChaseParticipantEntity,
    delta: Int,
): ChaseParticipantEntity {
    if (points.isEmpty()) return p
    val idx = points.indexOfFirst { it.id == p.pointId }
    val newIdx = if (idx < 0) {
        if (delta > 0) 0 else points.lastIndex
    } else {
        (idx + delta).coerceIn(0, points.lastIndex)
    }
    return p.copy(pointId = points[newIdx].id)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaseScreen(navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val participants by container.chaseDao.observeAll().collectAsState(emptyList())
    val points by container.chasePointDao.observeAll().collectAsState(emptyList())

    var showAddParticipant by remember { mutableStateOf(false) }
    var showAddPoint by remember { mutableStateOf(false) }

    val minMov = participants.minOfOrNull { it.mov } ?: 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = "追逐战小助手",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddParticipant = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.Add, contentDescription = "添加参与者") }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // —— 追逐点位 ——
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("追逐点位", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { showAddPoint = true }) {
                    Icon(Icons.Filled.Place, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加点位")
                }
            }
            if (points.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "还没有点位。点「添加点位」设定追逐路线上的地点（如 巷口 / 仓库 / 河边）。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                points.forEachIndexed { index, point ->
                    val count = participants.count { it.pointId == point.id }
                    PointRow(
                        point = point,
                        count = count,
                        canUp = index > 0,
                        canDown = index < points.lastIndex,
                        onUp = {
                            scope.launch {
                                val a = points[index]
                                val b = points[index - 1]
                                container.chasePointDao.update(a.copy(order = b.order))
                                container.chasePointDao.update(b.copy(order = a.order))
                            }
                        },
                        onDown = {
                            scope.launch {
                                val a = points[index]
                                val b = points[index + 1]
                                container.chasePointDao.update(a.copy(order = b.order))
                                container.chasePointDao.update(b.copy(order = a.order))
                            }
                        },
                        onDelete = {
                            scope.launch {
                                container.chasePointDao.delete(point)
                                participants.filter { it.pointId == point.id }.forEach {
                                    container.chaseDao.update(it.copy(pointId = 0))
                                }
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // —— 参与者 ——
            if (participants.isEmpty()) {
                EmptyState("还没有参与者，点击右下角添加", icon = Icons.Filled.DirectionsRun)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "参与者",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "最低 MOV $minMov · 行动点 = 1 + (MOV − $minMov)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            participants.forEach { p ->
                                val moved = moveParticipant(points, p, +1)
                                if (moved.pointId != p.pointId) container.chaseDao.update(moved)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Icon(Icons.Filled.DirectionsRun, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("全体前进一个点位")
                }
                participants.forEach { p ->
                    ChaseRow(
                        participant = p,
                        points = points,
                        ap = actionPoints(p.mov, minMov),
                        onPrev = { scope.launch { container.chaseDao.update(moveParticipant(points, p, -1)) } },
                        onNext = { scope.launch { container.chaseDao.update(moveParticipant(points, p, +1)) } },
                        onDelete = { scope.launch { container.chaseDao.delete(p) } },
                    )
                }
            }
        }
    }

    if (showAddPoint) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPoint = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("添加点位") },
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
                            val order = (points.maxOfOrNull { it.order } ?: -1) + 1
                            scope.launch { container.chasePointDao.insert(ChasePointEntity(name = name.trim(), order = order)) }
                        }
                        showAddPoint = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddPoint = false }) { Text("取消") } },
        )
    }

    if (showAddParticipant) {
        var name by remember { mutableStateOf("") }
        var mov by remember { mutableStateOf("8") }
        var role by remember { mutableStateOf(ChaseParticipantEntity.CHASE_QUARRY) }
        AlertDialog(
            onDismissRequest = { showAddParticipant = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("添加参与者") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mov, onValueChange = { mov = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("移动力 MOV") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ChaseParticipantEntity.CHASE_QUARRY to "逃亡者",
                            ChaseParticipantEntity.CHASE_PURSUER to "追捕者",
                        ).forEach { (r, label) ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            val m = mov.toIntOrNull() ?: 8
                            scope.launch { container.chaseDao.insert(ChaseParticipantEntity(name = name.trim(), mov = m, role = role)) }
                        }
                        showAddParticipant = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddParticipant = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun PointRow(
    point: ChasePointEntity,
    count: Int,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(point.name.ifBlank { "未命名点位" }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (count == 0) "无人" else "$count 人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onUp, enabled = canUp) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "上移", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDown, enabled = canDown) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "下移", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除点位", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ChaseRow(
    participant: ChaseParticipantEntity,
    points: List<ChasePointEntity>,
    ap: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(participant.name.ifBlank { "未命名" }, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${roleLabel(participant.role)} · MOV ${participant.mov} · 位置 ${pointName(points, participant.pointId)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "行动点 ×$ap",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "后退", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    pointName(points, participant.pointId),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "前进", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
