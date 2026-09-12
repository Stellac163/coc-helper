package com.cochelper.app.ui.screens.chase

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.ChaseParticipantEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.SectionTopBar
import kotlinx.coroutines.launch

private fun roleLabel(role: Int) = if (role == ChaseParticipantEntity.CHASE_PURSUER) "追捕者" else "逃亡者"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaseScreen(navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val participants by container.chaseDao.observeAll().collectAsStateWithLifecycle(emptyList())

    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = "追逐战小助手",
                leftIcon = Icons.Filled.DirectionsRun,
                onLeftClick = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.Add, contentDescription = "添加参与者") }
        },
    ) { padding ->
        if (participants.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                EmptyState("还没有参与者，点击右下角添加", icon = Icons.Filled.DirectionsRun)
            }
        } else {
            val sorted = participants.sortedByDescending { it.position }
            val maxPos = (sorted.maxOfOrNull { it.position } ?: 0).coerceAtLeast(1)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            participants.forEach { p ->
                                container.chaseDao.update(p.copy(position = p.position + p.mov))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Filled.DirectionsRun, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("全体前进一轮")
                }
                sorted.forEach { p ->
                    ChaseRow(
                        participant = p,
                        maxPos = maxPos,
                        onAdd = { scope.launch { container.chaseDao.update(p.copy(position = p.position + 1)) } },
                        onSub = { scope.launch { container.chaseDao.update(p.copy(position = (p.position - 1).coerceAtLeast(0))) } },
                        onDelete = { scope.launch { container.chaseDao.delete(p) } },
                    )
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var mov by remember { mutableStateOf("8") }
        var role by remember { mutableStateOf(ChaseParticipantEntity.CHASE_QUARRY) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
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
                        showAdd = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ChaseRow(
    participant: ChaseParticipantEntity,
    maxPos: Int,
    onAdd: () -> Unit,
    onSub: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(participant.name.ifBlank { "未命名" }, style = MaterialTheme.typography.bodyLarge)
                    Text("${roleLabel(participant.role)} · MOV ${participant.mov}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSub) { Icon(Icons.Filled.Remove, contentDescription = "后退", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("${participant.position}", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "前进", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp)),
            ) {
                val fraction = if (maxPos == 0) 0f else participant.position.toFloat() / maxPos
                Spacer(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}
