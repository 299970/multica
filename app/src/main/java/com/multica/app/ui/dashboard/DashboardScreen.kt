package com.multica.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multica.app.data.ws.WsState
import com.multica.app.ui.dashboard.tabs.AgentsTab
import com.multica.app.ui.dashboard.tabs.BossTab
import com.multica.app.ui.dashboard.tabs.DaemonsTab
import com.multica.app.ui.dashboard.tabs.IssuesTab
import com.multica.app.ui.dashboard.IssueDetailScreen
import com.multica.app.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onOpenSettings: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val issueNavId by vm.navigateIssueId.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val wsName = s.workspaces.firstOrNull { it.id == s.activeWorkspaceId }?.name ?: "multica"
    val (wsBg, wsLabel) = wsIndicator(s.wsState, s.isMock)

    Scaffold(
        topBar = {
            Column {
                // v0.3.1：工作区名 + WS 状态合并成一行，工作区名底色 = WS 状态色
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // 工作区名（带状态底色）
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(wsBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = wsName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            // 状态文字
                            Text(
                                text = wsLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = vm::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "设置")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        }
    ) { padding ->
        // 跳转到 Issue 详情页
        if (issueNavId != null) {
            val agentMap = s.agents.associate { it.id to it.name }
            IssueDetailScreen(
                vm = vm,
                agentNameOf = { id -> agentMap[id] },
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // v0.3.20: 老板 2026-06-08 需求 — 顶部 tab 颜色白色 + tab 样式
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color(0xFF1C1C1E),  // 暗色背景
                contentColor = Color.White,            // 文字白色
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color(0xFFB0B0B0),
                    // v0.3.13: 按 host 数（每台主机一个卡片），不按 runtime 数
                    text = { Text("Runtimes · ${s.runtimesHostCount}", color = Color.White) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color(0xFFB0B0B0),
                    text = { Text("Agents · ${s.agents.size}", color = Color.White) },
                )
                Tab(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color(0xFFB0B0B0),
                    text = { Text("Issues · ${s.activeIssuesCount}", color = Color.White) },
                )
                Tab(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color(0xFFB0B0B0),
                    text = { Text("Boss · ${s.bossCount}", color = Color.White) },
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> DaemonsTab(s = s, agents = s.agents, onRefresh = vm::refresh)
                    1 -> AgentsTab(s = s, runtimes = s.runtimes)
                    2 -> IssuesTab(s = s, agents = s.agents, onIssueClick = vm::openIssue)
                    3 -> BossTab(s = s, onRefresh = vm::refresh, onIssueClick = vm::openIssue)
                }
            }
        }
    }
}

/** 把 WS 状态翻译成 (底色, 状态文字) */
private fun wsIndicator(ws: WsState, isMock: Boolean): Pair<Color, String> = when {
    isMock -> Color(0xFF9CA3AF) to "示例数据"
    ws == WsState.Connected -> Color(0xFF22C55E) to "WS 已连接"
    ws == WsState.Connecting -> Color(0xFFEAB308) to "WS 连接中"
    ws == WsState.Failed -> Color(0xFFEF4444) to "WS 失败"
    else -> Color(0xFF9CA3AF) to "WS 未连接"
}
