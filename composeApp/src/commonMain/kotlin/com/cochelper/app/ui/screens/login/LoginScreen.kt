package com.cochelper.app.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cochelper.app.data.AppSettings
import com.cochelper.app.platform.HttpPhase
import com.cochelper.app.platform.HttpProgress
import com.cochelper.app.platform.compressImageDataUrl
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.LoadedImage
import com.cochelper.app.ui.components.SectionTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val settings by container.settings.settings.collectAsState(AppSettings())

    var token by remember { mutableStateOf(settings.githubToken) }
    var repoName by remember { mutableStateOf(settings.repoName) }
    var nickname by remember { mutableStateOf(settings.nickname) }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<HttpProgress?>(null) }

    fun notify(msg: String) = scope.launch { snackbar.showSnackbar(msg) }

    fun doLogin() {
        scope.launch {
            loading = true
            val repo = repoName.ifBlank { "coc-helper-backup" }
            container.github.getUser(token).onSuccess { user ->
                container.settings.setGithubToken(token)
                container.settings.setRepoName(repo)
                if (nickname.isNotBlank()) container.settings.setNickname(nickname)
                // 仅在尚未设置自定义头像时用 GitHub 头像兜底，避免覆盖用户自己上传的头像
                if (settings.avatarUri.isBlank() && user.avatarUrl.isNotBlank()) {
                    container.settings.setAvatarUri(user.avatarUrl)
                }
                notify("登录成功：${user.login}")
            }.onFailure {
                notify("登录失败：${it.message}")
            }
            loading = false
        }
    }

    fun upload() {
        scope.launch {
            loading = true
            progress = null
            val repo = repoName.ifBlank { "coc-helper-backup" }
            container.settings.setRepoName(repo)
            container.uploadToCloud(token, repo) { progress = it }.onSuccess { msg ->
                notify(msg)
            }.onFailure { notify("上传失败：${it.message}") }
            loading = false
            progress = null
        }
    }

    fun restore() {
        scope.launch {
            loading = true
            progress = null
            val repo = repoName.ifBlank { "coc-helper-backup" }
            container.settings.setRepoName(repo)
            container.restoreFromCloud(token, repo) { progress = it }.onSuccess { msg ->
                notify(msg)
            }.onFailure { notify("恢复失败：${it.message}") }
            loading = false
            progress = null
        }
    }

    fun pickAvatar() {
        scope.launch {
            val picked = container.files.pickFile(arrayOf("image/*"))
            if (picked != null) {
                val compressed = compressImageDataUrl(picked.dataUrl, 256, 0.82)
                container.settings.setAvatarUri(compressed)
                notify("头像已更新")
            }
        }
    }

    fun logout() {
        scope.launch {
            container.settings.setGithubToken("")
            token = ""
            notify("已退出登录")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SectionTopBar(
                title = "登录与同步",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "使用 GitHub 私有仓库备份。令牌仅保存在本机。\n「上传」用本地覆盖云端；「从云端恢复」用云端覆盖本地（单向，不合并）。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("GitHub 访问令牌") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = repoName,
                onValueChange = { repoName = it },
                label = { Text("仓库名（私有）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("昵称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LoadedImage(
                    uri = settings.avatarUri.ifBlank { null },
                    modifier = Modifier.size(72.dp),
                    icon = Icons.Filled.Person,
                    corner = 36.dp,
                )
                OutlinedButton(onClick = { pickAvatar() }) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("更换头像")
                }
            }

            if (loading) {
                SyncProgressBar(progress)
            }

            Button(
                onClick = { doLogin() },
                enabled = token.isNotBlank() && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Icon(Icons.Filled.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("登录并验证")
            }

            if (settings.githubToken.isNotBlank() || token.isNotBlank()) {
                Button(
                    onClick = { upload() },
                    enabled = token.isNotBlank() && !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("上传（覆盖云端）")
                }
                OutlinedButton(
                    onClick = { restore() },
                    enabled = token.isNotBlank() && !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("从云端恢复（覆盖本地）")
                }
                TextButton(onClick = { logout() }) {
                    Icon(Icons.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("退出登录")
                }
            }
        }
    }
}

/** 上传/下载进度条：有总字节数时显示百分比，否则显示不确定态。 */
@Composable
private fun SyncProgressBar(progress: HttpProgress?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (progress != null && progress.total > 0) {
            LinearProgressIndicator(
                progress = { (progress.loaded.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
            val verb = if (progress.phase == HttpPhase.UPLOAD) "上传" else "下载"
            Text(
                text = "${verb}中 ${(progress.loaded * 100 / progress.total).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
            Text(
                text = "处理中…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
