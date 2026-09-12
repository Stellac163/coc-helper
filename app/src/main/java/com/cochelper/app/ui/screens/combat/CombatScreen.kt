package com.cochelper.app.ui.screens.combat

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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.CombatantEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.SectionTopBar
import kotlinx.coroutines.launch

private fun statusLabel(s: Int) = when (s) {
    CombatantEntity.COMBATANT_UNCONSCIOUS -> "昏迷"
    CombatantEntity.COMBATANT_DEAD -> "死亡"
    else -> "存活"
}

@Composable
private fun statusColor(s: Int) = when (s) {
    CombatantEntity.COMBATANT_UNCONSCIOUS -> MaterialTheme.colorScheme.tertiary
    CombatantEntity.COMBATANT_DEAD -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombatScreen(navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val combatants by container.combatantDao.observeAll().collectAsStateWithLifecycle(emptyList())

    var turn by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CombatantEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = "战斗轮小助手",
                leftIcon = Icons.Filled.Shield,
                onLeftClick = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.Add, contentDescription = "添加参战者") }
        },
    ) { padding ->
        if (combatants.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                EmptyState("还没有参战者，点击右下角添加", icon = Icons.Filled.Shield)
            }
        } else {
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
                    onClick = { turn++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("下一个行动")
                }
                val currentIndex = if (combatants.isEmpty()) 0 else turn % combatants.size
                combatants.forEachIndexed { index, c ->
                    CombatantRow(
                        combatant = c,
                        isCurrent = index == currentIndex,
                        onClick = { editing = c },
                    )
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var dex by remember { mutableStateOf("50") }
        var hp by remember { mutableStateOf("10") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("添加参战者") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dex, onValueChange = { dex = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("敏捷 DEX") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hp, onValueChange = { hp = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("生命值 HP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            val d = dex.toIntOrNull() ?: 50
                            val h = hp.toIntOrNull() ?: 10
                            scope.launch { container.combatantDao.insert(CombatantEntity(name = name.trim(), dex = d, hp = h, maxHp = h)) }
                        }
                        showAdd = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }

    editing?.let { c ->
        CombatantEditDialog(
            combatant = c,
            onDismiss = { editing = null },
            onSave = { updated -> scope.launch { container.combatantDao.update(updated) }; editing = null },
            onDelete = { scope.launch { container.combatantDao.delete(c) }; editing = null },
        )
    }
}

@Composable
private fun CombatantRow(combatant: CombatantEntity, isCurrent: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    combatant.name.ifBlank { "未命名" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "DEX ${combatant.dex}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${combatant.hp}/${combatant.maxHp} HP",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = statusColor(combatant.status).copy(alpha = 0.15f),
            ) {
                Text(
                    statusLabel(combatant.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(combatant.status),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CombatantEditDialog(
    combatant: CombatantEntity,
    onDismiss: () -> Unit,
    onSave: (CombatantEntity) -> Unit,
    onDelete: () -> Unit,
) {
    var hp by remember(combatant.id) { mutableIntStateOf(combatant.hp) }
    var status by remember(combatant.id) { mutableIntStateOf(combatant.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(combatant.name.ifBlank { "未命名" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("生命值", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { hp = (hp - 5).coerceAtLeast(0) }) { Icon(Icons.Filled.Remove, contentDescription = "-5") }
                        Text("${hp}/${combatant.maxHp}", style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = { hp = (hp + 5).coerceAtMost(combatant.maxHp) }) { Icon(Icons.Filled.Add, contentDescription = "+5") }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("状态", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            CombatantEntity.COMBATANT_ALIVE to "存活",
                            CombatantEntity.COMBATANT_UNCONSCIOUS to "昏迷",
                            CombatantEntity.COMBATANT_DEAD to "死亡",
                        ).forEach { (s, label) ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(combatant.copy(hp = hp, status = status)) }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
