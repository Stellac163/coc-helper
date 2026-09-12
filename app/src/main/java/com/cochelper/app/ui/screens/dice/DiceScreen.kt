package com.cochelper.app.ui.screens.dice

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cochelper.app.domain.DiceEngine
import com.cochelper.app.domain.DiceResult
import com.cochelper.app.domain.SuccessLevel
import com.cochelper.app.ui.components.AppBottomBar
import com.cochelper.app.ui.components.navigateToTab
import com.cochelper.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var faces by remember { mutableStateOf("100") }
    var count by remember { mutableStateOf("1") }
    var skill by remember { mutableStateOf("") }
    var bonus by remember { mutableStateOf("0") }
    var penalty by remember { mutableStateOf("0") }
    var opponent by remember { mutableStateOf("") }

    var result by remember { mutableStateOf<DiceResult?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AppBottomBar(currentRoute = Routes.DICE) { route -> navController.navigateToTab(route) }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ResultContainer(result = result)

            // 骰子面数 + 骰子数目
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = faces,
                    onValueChange = { faces = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("骰子面数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("骰子数目") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            // 技能数值
            OutlinedTextField(
                value = skill,
                onValueChange = { skill = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("技能数值") },
                leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 奖励骰 + 惩罚骰
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = bonus,
                    onValueChange = { bonus = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("奖励骰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = penalty,
                    onValueChange = { penalty = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("惩罚骰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            // 对抗者技能数值
            OutlinedTextField(
                value = opponent,
                onValueChange = { opponent = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("对抗者技能数值") },
                leadingIcon = { Icon(Icons.Filled.Forum, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val f = faces.toIntOrNull()?.coerceIn(1, 10000) ?: 100
                    val c = count.toIntOrNull()?.coerceIn(1, 100) ?: 1
                    val s = skill.toIntOrNull()
                    val b = bonus.toIntOrNull()?.coerceIn(0, 10) ?: 0
                    val p = penalty.toIntOrNull()?.coerceIn(0, 10) ?: 0
                    val o = opponent.toIntOrNull()
                    val r = DiceEngine.roll(f, c, s, b, p, o)
                    result = r

                    val msg = when {
                        r.outcome != null -> "${r.level?.label}（${outcomeText(r.outcome)}）"
                        r.level != null -> "${r.level.label}！"
                        else -> "结果：${r.total}"
                    }
                    scope.launch {
                        snackbar.showSnackbar(
                            message = msg,
                            actionLabel = "确定",
                            duration = SnackbarDuration.Short,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("投掷", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun outcomeText(o: com.cochelper.app.domain.OpposedOutcome?): String = when (o) {
    com.cochelper.app.domain.OpposedOutcome.WIN -> "对抗胜出"
    com.cochelper.app.domain.OpposedOutcome.LOSE -> "对抗落败"
    com.cochelper.app.domain.OpposedOutcome.DRAW -> "对抗平局"
    null -> ""
}

@Composable
private fun ResultContainer(result: DiceResult?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
            )
            .padding(24.dp),
    ) {
        if (result == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "设置好参数后点击“投掷”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                result.level?.let { level ->
                    Text(
                        level.label,
                        style = MaterialTheme.typography.displaySmall,
                        color = levelColor(level),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (result.rollValue != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "投掷点数：${result.rollValue}",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } ?: run {
                    Text(
                        "${result.total}",
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    result.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun levelColor(level: SuccessLevel): Color = when (level) {
    SuccessLevel.CRITICAL -> MaterialTheme.colorScheme.primary
    SuccessLevel.EXTREME -> MaterialTheme.colorScheme.tertiary
    SuccessLevel.HARD -> MaterialTheme.colorScheme.primary
    SuccessLevel.SUCCESS -> MaterialTheme.colorScheme.onSurface
    SuccessLevel.FAILURE -> MaterialTheme.colorScheme.onSurfaceVariant
    SuccessLevel.FUMBLE -> MaterialTheme.colorScheme.error
}
