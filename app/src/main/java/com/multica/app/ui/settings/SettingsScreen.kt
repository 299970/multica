package com.multica.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multica.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务器设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "连接自托管 Multica 服务器",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "1. 启动 multica 服务（如 docker-compose up -d）\n" +
                "2. 在服务端创建 PAT：Settings → Personal Access Tokens\n" +
                "3. 填入下方信息，保存后会立即测试连接",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // v0.3.29: 内网地址（老板需求 — 内网优先）
            OutlinedTextField(
                value = s.lanUrl,
                onValueChange = vm::setLanUrl,
                label = { Text("内网地址（优先）") },
                placeholder = { Text("http://172.26.28.80:9090") },
                supportingText = { Text("内网连通时使用；失败自动 fallback 到域名", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            // v0.3.29: 域名地址（老板需求 — 内网不通时使用）
            OutlinedTextField(
                value = s.wanUrl,
                onValueChange = vm::setWanUrl,
                label = { Text("域名地址（fallback）") },
                placeholder = { Text("https://multica.299970.xyz") },
                supportingText = { Text("内网不通时使用（HTTPS）", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            // v0.3.29: 兼容字段（保留，万一老板想直接指定一个 URL）
            // v0.3.30: 删掉了"Server URL 兼容字段"（老板反馈：和内网地址重复）
            // 现在只用：内网地址 / 域名地址 2 个字段

            OutlinedTextField(
                value = s.pat,
                onValueChange = vm::setPat,
                label = { Text("Personal Access Token (mul_...)") },
                placeholder = { Text("mul_xxxxxxxxxxxxxxxxxxxxxxxxxx") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = s.workspaceId,
                onValueChange = vm::setWorkspaceId,
                label = { Text("Workspace ID (留空自动选第一个)") },
                placeholder = { Text("5fb87ac7-23b5-4a7a-81fa-ed295a54545d") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            s.errorMessage?.let {
                Text(
                    "✗ $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            s.testResult?.let {
                Text(
                    "✓ $it",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = vm::save,
                enabled = !s.isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (s.isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp).fillMaxWidth(0.05f),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                }
                Text(if (s.isWorking) "测试中…" else "保存并连接")
            }

            Text(
                "提示：PAT 加密保存到 EncryptedSharedPreferences（AndroidKeyStore）。\n" +
                "WebSocket 通过 /ws?token=<PAT> 连接，状态变更秒级推送。",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
