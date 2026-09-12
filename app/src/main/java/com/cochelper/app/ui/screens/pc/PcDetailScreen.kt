package com.cochelper.app.ui.screens.pc

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.PcEntity
import com.cochelper.app.data.local.SkillItem
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
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val pc by container.pcDao.observeById(pcId).collectAsStateWithLifecycle(null)

    var draft by remember(pcId) { mutableStateOf<PcEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var fabExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // 首次加载时用数据库数据初始化草稿
    if (draft == null && pc != null) draft = pc

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            draft = draft?.copy(imageUri = uri.toString())
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SectionTopBar(
                title = "人物详情",
                leftIcon = Icons.Filled.Person,
                onLeftClick = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            FabMenu(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                fabIcon = Icons.Filled.Edit,
                fabContentDescription = "编辑操作",
                items = listOf(
                    FabMenuItem(Icons.Filled.Save, "保存") { save(); fabExpanded = false },
                    FabMenuItem(Icons.Filled.AddAPhoto, "更换头像") {
                        avatarPicker.launch(arrayOf("image/*"))
                        fabExpanded = false
                    },
                    FabMenuItem(Icons.Filled.Delete, "删除") {
                        confirmDelete = true
                        fabExpanded = false
                    },
                ),
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

            TabRow(selectedTabIndex = selectedTab) {
                listOf("属性", "技能", "背景").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) },
                    )
                }
            }

            when (selectedTab) {
                0 -> BasicTab(d) { draft = it }
                1 -> SkillsTab(d) { draft = it }
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
                    scope.launch { container.pcDao.delete(d) }
                    confirmDelete = false
                    navController.navigateUp()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun AvatarHeader(imageUri: String, name: String, player: String) {
    Column {
        LoadedImage(
            uri = imageUri.ifBlank { null },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            icon = Icons.Filled.Person,
            corner = 20.dp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            name.ifBlank { "人物名称" },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (player.isNotBlank()) {
            Text(
                "玩家 $player",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun BasicTab(pc: PcEntity, onChange: (PcEntity) -> Unit) {
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
    }
}

@Composable
private fun SkillsTab(pc: PcEntity, onChange: (PcEntity) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        pc.skills.forEachIndexed { index, skill ->
            SkillRow(skill = skill, onName = { name ->
                onChange(pc.copy(skills = pc.skills.toMutableList().also { it[index] = it[index].copy(name = name) }))
            }, onValue = { value ->
                onChange(pc.copy(skills = pc.skills.toMutableList().also { it[index] = it[index].copy(value = value) }))
            }, onDelete = {
                onChange(pc.copy(skills = pc.skills.filterIndexed { i, _ -> i != index }))
            })
        }
        TextButton(onClick = { onChange(pc.copy(skills = pc.skills + SkillItem("", 0))) }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("添加技能")
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
private fun SkillRow(
    skill: SkillItem,
    onName: (String) -> Unit,
    onValue: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = skill.name,
            onValueChange = onName,
            label = { Text("技能") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = skill.value.takeIf { it != 0 }?.toString() ?: "",
            onValueChange = { s -> onValue(s.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0) },
            label = { Text("数值") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(88.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
        }
    }
}
