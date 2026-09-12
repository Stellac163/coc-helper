package com.cochelper.app.ui.screens.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.FloatingToolbar
import com.cochelper.app.ui.components.MarkdownText
import com.cochelper.app.ui.components.SectionTopBar
import com.cochelper.app.ui.components.ToolbarAction
import com.cochelper.app.ui.components.applyMarkup
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroEditorScreen(moduleId: Long, navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val module by container.moduleDao.observeById(moduleId).collectAsStateWithLifecycle(null)

    var editing by remember { mutableStateOf(false) }
    var textField by remember { mutableStateOf(TextFieldValue("")) }

    fun startEditing() {
        textField = TextFieldValue(module?.introText ?: "")
        editing = true
    }

    fun save() {
        module?.let { m ->
            scope.launch { container.moduleDao.update(m.copy(introText = textField.text)) }
        }
    }

    val toolbarActions = listOf(
        ToolbarAction(Icons.Filled.FormatBold, "加粗") { textField = applyMarkup(textField, "**") },
        ToolbarAction(Icons.Filled.FormatItalic, "斜体") { textField = applyMarkup(textField, "*") },
        ToolbarAction(Icons.Filled.FormatUnderlined, "下划线") { textField = applyMarkup(textField, "__") },
        ToolbarAction(Icons.Filled.AttachFile, "附件") {
            textField = textField.copy(text = textField.text + "\n[附件]")
        },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SectionTopBar(
                title = "简介与招募",
                leftIcon = Icons.Filled.Chat,
                onLeftClick = { navController.navigateUp() },
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
                        value = textField,
                        onValueChange = { v ->
                            textField = v
                            save()
                        },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("输入简介与招募信息…") },
                    )
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (module?.introText.isNullOrBlank()) {
                            Text(
                                "还没有内容，点击右下角编辑按钮开始书写",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            MarkdownText(module!!.introText)
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
                SmallFloatingActionButton(
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
}
