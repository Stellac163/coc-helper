package com.cochelper.app.ui.screens.pc

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.AttributeItem
import com.cochelper.app.data.local.PcEntity
import com.cochelper.app.data.local.SkillItem
import com.cochelper.app.domain.StImport
import com.cochelper.app.platform.compressImageDataUrl
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.FabMenu
import com.cochelper.app.ui.components.FabMenuItem
import com.cochelper.app.ui.components.LoadedImage
import com.cochelper.app.ui.components.SectionTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcDetailScreen(pcId: Long, showAvatar: Boolean, navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val pc by container.pcDao.observeById(pcId).collectAsState(null)

    var draft by remember(pcId) { mutableStateOf<PcEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var fabExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var importOpen by remember { mutableStateOf(false) }
    var editingSkill by remember { mutableStateOf<Int?>(null) }
    var editingAttr by remember { mutableStateOf<Int?>(null) }

    // 首次加载时用数据库数据初始化草稿
    if (draft == null && pc != null) draft = pc

    fun pickAvatar() {
        scope.launch {
            val picked = container.files.pickFile(arrayOf("image/*"))
            if (picked != null) {
                val compressed = compressImageDataUrl(picked.dataUrl, 512, 0.82)
                draft = draft?.copy(imageUri = compressed)
            }
        }
    }

    val d = draft
    if (d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState("角色不存在或已被删除")
        }
        return
    }

    fun save() {
        scope.launch {
            container.pcDao.update(d)
            snackbar.showSnackbar("已保存")
        }
    }

    fun updateSkill(index: Int, name: String, value: Int) {
        draft = d.copy(skills = d.skills.toMutableList().also { it[index] = it[index].copy(name = name, value = value) })
    }

    fun deleteSkill(index: Int) {
        draft = d.copy(skills = d.skills.filterIndexed { i, _ -> i != index })
    }

    fun addSkill() {
        draft = d.copy(skills = d.skills + SkillItem("", 0))
    }

    fun updateAttr(index: Int, value: Int) {
        draft = d.copy(attributes = d.attributes.toMutableList().also { it[index] = it[index].copy(value = value) })
    }

    fun applyImport(result: StImport.Result) {
        val mergedAttrs = d.attributes.map { attr ->
            result.attributes[attr.name]?.let { attr.copy(value = it) } ?: attr
        }
        val skillMap = d.skills.associateBy { it.name }.toMutableMap()
        result.skills.forEach { s ->
            if (s.name.isNotBlank()) skillMap[s.name] = s
        }
        draft = d.copy(attributes = mergedAttrs, skills = skillMap.values.toList())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SectionTopBar(
                title = "人物详情",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            FabMenu(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                fabIcon = Icons.Filled.Edit,
                fabContentDescription = "编辑操作",
                items = buildList {
                    add(FabMenuItem(Icons.Filled.Save, "保存") { save(); fabExpanded = false })
                    if (showAvatar) {
                        add(FabMenuItem(Icons.Filled.AddAPhoto, "更换头像") { pickAvatar(); fabExpanded = false })
                    }
                    add(FabMenuItem(Icons.Filled.Delete, "删除") { confirmDelete = true; fabExpanded = false })
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (showAvatar) {
                AvatarHeader(
                    imageUri = d.imageUri,
                    name = d.name,
                    player = d.player,
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                listOf("属性", "技能", "背景").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) },
                    )
                }
            }

            when (selectedTab) {
                0 -> AttributesTab(d, onImport = { importOpen = true }, onEditAttr = { editingAttr = it }) { draft = it }
                1 -> SkillsTab(
                    pc = d,
                    onImport = { importOpen = true },
                    onEditSkill = { editingSkill = it },
                    onAdd = { addSkill() },
                )
                2 -> BackgroundTab(d) { draft = it }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("删除角色") },
            text = { Text("确定要删除“${d.name}”吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        container.pcDao.delete(d)
                        navController.navigateUp()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }

    if (importOpen) {
        StImportDialog(
            onDismiss = { importOpen = false },
            onConfirm = { result ->
                applyImport(result)
                importOpen = false
            },
        )
    }

    editingSkill?.let { index ->
        val skill = d.skills.getOrNull(index) ?: return@let
        SkillEditDialog(
            skill = skill,
            onDismiss = { editingSkill = null },
            onSave = { name, value ->
                updateSkill(index, name, value)
                editingSkill = null
            },
            onDelete = {
                deleteSkill(index)
                editingSkill = null
            },
        )
    }

    editingAttr?.let { index ->
        val attr = d.attributes.getOrNull(index) ?: return@let
        AttrEditDialog(
            attr = attr,
            onDismiss = { editingAttr = null },
            onSave = { value ->
                updateAttr(index, value)
                editingAttr = null
            },
        )
    }
}

@Composable
private fun AvatarHeader(imageUri: String, name: String, player: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadedImage(
            uri = imageUri.ifBlank { null },
            modifier = Modifier.size(140.dp),
            icon = Icons.Filled.Person,
            corner = 0.dp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            name.ifBlank { "人物名称" },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (player.isNotBlank()) {
            Text(
                "玩家 $player",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AttributesTab(
    pc: PcEntity,
    onImport: () -> Unit,
    onEditAttr: (Int) -> Unit,
    onChange: (PcEntity) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        LabeledField("姓名", pc.name) { onChange(pc.copy(name = it)) }
        LabeledField("玩家", pc.player) { onChange(pc.copy(player = it)) }
        LabeledField("性别", pc.gender) { onChange(pc.copy(gender = it)) }
        LabeledField("年龄", pc.age) { onChange(pc.copy(age = it)) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("属性", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onImport) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("导入骰娘指令")
            }
        }

        pc.attributes.withIndex().chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { (index, attr) ->
                    AttrCell(
                        item = attr,
                        onClick = { onEditAttr(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AttrCell(item: AttributeItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                item.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                item.value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillsTab(
    pc: PcEntity,
    onImport: () -> Unit,
    onEditSkill: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onImport) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("导入骰娘指令")
            }
            TextButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("添加技能")
            }
        }

        pc.skills.withIndex().chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { (index, skill) ->
                    SkillCell(
                        skill = skill,
                        onClick = { onEditSkill(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillCell(skill: SkillItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                skill.name.ifBlank { "未命名" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                skill.value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BackgroundTab(pc: PcEntity, onChange: (PcEntity) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        LabeledField("外貌", pc.appearance) { onChange(pc.copy(appearance = it)) }
        LabeledField("信仰", pc.beliefs) { onChange(pc.copy(beliefs = it)) }
        LabeledField("重要之人", pc.importantPlaces) { onChange(pc.copy(importantPlaces = it)) }
        LabeledField("贵重物品", pc.valuables) { onChange(pc.copy(valuables = it)) }
        LabeledField("性格", pc.traits) { onChange(pc.copy(traits = it)) }
        LabeledField("伤痕", pc.wounds) { onChange(pc.copy(wounds = it)) }
        LabeledField("恐惧症", pc.phobias) { onChange(pc.copy(phobias = it)) }
        LabeledField("背景故事", pc.background, minLines = 3) { onChange(pc.copy(background = it)) }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    minLines: Int = 1,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = minLines <= 1,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SkillEditDialog(
    skill: SkillItem,
    onDismiss: () -> Unit,
    onSave: (name: String, value: Int) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(skill) { mutableStateOf(skill.name) }
    var value by remember(skill) { mutableStateOf(skill.value.takeIf { it != 0 }?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("编辑技能") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("技能名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { s -> value = s.filter { it.isDigit() }.take(3) },
                    label = { Text("数值") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("删除此技能", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), value.toIntOrNull() ?: 0) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AttrEditDialog(
    attr: AttributeItem,
    onDismiss: () -> Unit,
    onSave: (value: Int) -> Unit,
) {
    var value by remember(attr) { mutableStateOf(attr.value.takeIf { it != 0 }?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("编辑属性：${attr.name}") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { s -> value = s.filter { it.isDigit() }.take(3) },
                label = { Text("数值") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.toIntOrNull() ?: 0) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun StImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (StImport.Result) -> Unit,
) {
    var raw by remember { mutableStateOf("") }
    val result = remember(raw) { runCatching { StImport.parse(raw) }.getOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("导入骰娘指令 (.st)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it },
                    label = { Text("粘贴 .st 指令") },
                    placeholder = { Text("例如：.st 力量50 敏捷70 意志65 侦查70") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (raw.isNotBlank()) {
                    Text(
                        when {
                            result == null -> "无法解析输入"
                            else -> "识别到 ${result.attributes.size} 项属性、${result.skills.size} 项技能"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { result?.let(onConfirm) },
                enabled = result != null && (result.attributes.isNotEmpty() || result.skills.isNotEmpty()),
            ) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
