package com.multica.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
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
    // v0.3.28 修：之前 0=Boss 假设错，实际顺序是 0=Runtimes, 1=Agents, 2=Issues, 3=Boss
    // 老板 2026-06-08 反馈："默认打开是 Runtimes，不是 Agents"
    // 现在默认 = 1 = Agents Tab
    var tab by remember { mutableIntStateOf(1) }
    val wsName = s.workspaces.firstOrNull { it.id == s.activeWorkspaceId }?.name ?: "multica"
    // v0.3.29: 标题颜色 = 内网绿 / 域名蓝 / 断开红 / mock 灰
    val (wsBg, wsLabel) = wsIndicator(s.netState, s.isMock)
    // v0.3.29: 工作区 dropdown 状态
    var wsMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                // v0.3.21: 简化 TopAppBar（去掉状态文字行）— 让顶部不占大块空间
                TopAppBar(
                    title = {
                        // v0.3.29: 标题可点击 → 弹工作区 dropdown
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(wsBg)
                                    .clickable { wsMenuOpen = true }
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = wsName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "切换工作区",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = wsMenuOpen,
                                onDismissRequest = { wsMenuOpen = false },
                            ) {
                                if (s.workspaces.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("暂无工作区") },
                                        onClick = { wsMenuOpen = false },
                                    )
                                } else {
                                    s.workspaces.forEach { ws ->
                                        val isCurrent = ws.id == s.activeWorkspaceId
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isCurrent) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF22C55E),
                                                            modifier = Modifier.size(16.dp),
                                                        )
                                                        Spacer(Modifier.size(4.dp))
                                                    }
                                                    Text(
                                                        ws.name,
                                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                                    )
                                                }
                                            },
                                            onClick = {
                                                wsMenuOpen = false
                                                if (!isCurrent) vm.setWorkspace(ws.id)
                                            },
                                        )
                                    }
                                }
                            }
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
                    windowInsets = WindowInsets(0),  // v0.3.21: 不要状态栏 inset，让 content 占满屏幕
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

/** v0.3.29: 把网络状态翻译成 (底色, 状态文字)
 *  老板需求 2026-06-08: 内网=绿，域名=蓝，无法连接=红 */
private fun wsIndicator(net: com.multica.app.data.net.NetworkManager.NetState, isMock: Boolean): Pair<Color, String> = when {
    isMock -> Color(0xFF9CA3AF) to "示例数据"
    net is com.multica.app.data.net.NetworkManager.NetState.Internal -> Color(0xFF22C55E) to "内网 ●"  // 绿
    net is com.multica.app.data.net.NetworkManager.NetState.External -> Color(0xFF3B82F6) to "域名 ●"  // 蓝
    net is com.multica.app.data.net.NetworkManager.NetState.Failed -> Color(0xFFEF4444) to "断开 ✕"   // 红
    else -> Color(0xFF9CA3AF) to "检测中…"                                                     // 灰
}
