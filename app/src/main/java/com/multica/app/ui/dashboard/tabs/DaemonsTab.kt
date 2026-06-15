package com.multica.app.ui.dashboard.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multica.app.data.model.Agent
import com.multica.app.data.model.DailyUsage
import com.multica.app.data.model.Runtime
import com.multica.app.ui.components.StatusDot
import com.multica.app.ui.components.TokenBarChart
import com.multica.app.ui.components.statusLabel
import com.multica.app.ui.viewmodel.DashboardUiState

/**
 * Runtimes Tab — v0.3.9
 * 老板 2026-06-08 需求：一台主机一个卡片
 * v0.3.14 老板 2026-06-08 需求：**卡片自适应高度**（撑满屏幕，不要下面空）
 *
 * 实现：
 *  - 把所有 runtimes 按 host (deviceName) 分组 → 每台 host 一张卡
 *  - 卡片显示 host name + 状态（host 在线 = 至少一个 runtime 在线 / 离线 = 所有 runtime 离线）
 *  - 用 BoxWithConstraints 计算每张卡应得高度（总可用高 / 卡片数），让卡片**均匀**撑满
 *  - 当内容比屏小（4 台主机）时，每张卡撑到 1/4 屏高，避免下面空
 *  - 当内容比屏大（>6 卡片）时，自然 wrap 滚动
 */
@Composable
fun DaemonsTab(
    s: DashboardUiState,
    agents: List<Agent> = emptyList(),
    onRefresh: () -> Unit = {},
    onClickRuntime: (Runtime) -> Unit = {},
) {
    if (s.runtimes.isEmpty() && !s.isLoading) {
        EmptyTab(text = "还没有 runtime — 在主机上运行 `multica daemon start`")
        return
    }
    val hostGroups = remember(s.runtimes) { groupRuntimesByHost(s.runtimes) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 算每张卡应得高度：可用高度 - 顶部 8dp - 底部 8dp - 卡片间距 8dp*N - TokenBarChart 高度
        // v0.3.35: TokenBarChart 76dp 必须在算 perCardH 时扣掉，否则卡片会挤压
        val totalH = maxHeight
        val paddingV = 16.dp
        val cardGap = 8.dp
        val chartHeight = 80.dp   // 64dp bars + 12dp date labels + 4dp padding
        val perCardH = if (hostGroups.isNotEmpty()) {
            (totalH - paddingV - cardGap * (hostGroups.size - 1) - chartHeight) / hostGroups.size
        } else 0.dp
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // v0.3.34 老板 2026-06-09 需求：顶部 30 天每日 token 用量柱状图
            // 不卡片化（不包 Card），只用 Row 占据小条空间
            item(key = "__token_chart__") {
                val chartState = remember { mutableStateOf<List<DailyUsage>>(emptyList()) }
                val chartScope = rememberCoroutineScope()
                LaunchedEffect(s.activeWorkspaceId) {
                    val slug = s.activeWorkspaceSlug() ?: return@LaunchedEffect
                    if (slug.isNotBlank()) {
                        // v0.3.34 直接走 ServiceLocator 单例 repo；优先 /api/dashboard/usage 否则 mock
                        val r = com.multica.app.di.ServiceLocator.repo
                            .dailyUsage(slug, days = 30)
                        r.onSuccess { chartState.value = it.take(30) }
                            .onFailure { chartState.value = emptyList() }
                    }
                }
                TokenBarChart(
                    data = chartState.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
            items(hostGroups, key = { it.hostName }) { group ->
                HostCard(
                    group = group,
                    agents = agents,
                    minHeight = perCardH,
                    onClick = { onClickRuntime(group.runtimes.first()) },
                )
            }
        }
    }
}

/** 主机分组：每台 host 一组，包含该 host 上所有 runtime + 推断的 state */
data class HostGroup(
    val hostName: String,            // e.g. "mac", "p1", "q6a", "orangepi4pro"
    val runtimes: List<Runtime>,     // 该 host 上所有 runtime（Openclaw/Claude/Hermes/Opencode）
) {
    /** 主机在线状态：至少一个 runtime online → online；全 offline → offline */
    val aggregateState: String
        get() = when {
            runtimes.any { isOnline(it.state) } -> "online"
            runtimes.any { it.state.lowercase() in setOf("starting", "connecting", "busy") } -> "starting"
            runtimes.all { it.state.lowercase() in setOf("offline", "stopped", "disconnected") } -> "offline"
            else -> "unknown"
        }
    /** 状态优先级（老板 2026-06-08 要求：在先按状态，在线>异常>离线） */
    val stateRank: Int get() = when (aggregateState) {
        "online" -> 0
        "starting" -> 1
        "offline" -> 2
        else -> 3
    }
}

private fun isOnline(state: String): Boolean = state.lowercase() in
    setOf("online", "running", "active", "connected", "starting")

/** 从 runtime.name (e.g. "Openclaw (mac)") 提取括号里的 host */
private fun extractHost(rt: Runtime): String {
    val name = rt.name
    val idx = name.lastIndexOf('(')
    if (idx >= 0 && name.endsWith(")")) {
        return name.substring(idx + 1, name.length - 1)
    }
    // fallback: 用 deviceName 第一段
    return rt.deviceName?.substringBefore(" · ")?.takeIf { it.isNotBlank() } ?: rt.id.take(6)
}

/** 按 host 分组，按（状态优先级 + host name）排序（老板 2026-06-08 要求：在线>异常>离线，其次名称） */
private fun groupRuntimesByHost(runtimes: List<Runtime>): List<HostGroup> {
    return runtimes
        .groupBy { extractHost(it) }
        .map { (host, list) -> HostGroup(hostName = host, runtimes = list) }
        .sortedWith(compareBy({ it.stateRank }, { it.hostName }))
}

@Composable
private fun HostCard(
    group: HostGroup,
    agents: List<Agent>,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit,
    onStatusClick: () -> Unit = {},  // v0.3.29: 点状态圆点显示详情
) {
    val state = group.aggregateState
    // 该 host 上所有 AI agent（任意 runtime 上的）
    val hostAgentIds = group.runtimes.map { it.id }.toSet()
    val hostAgents = agents.filter { it.runtimeId in hostAgentIds }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight), // v0.3.14: 自适应高度（屏高/N）
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 第 1 行：host 名称 + 状态圆点（可点击）+ 状态文字
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // v0.3.29: 状态圆点可点击 → 弹详情
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onStatusClick)
                    .padding(2.dp),
            ) {
                StatusDot(state = state, modifier = Modifier.padding(2.dp))
            }
            Text(
                text = "🖥  ${group.hostName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = statusLabel(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 第 2 行：IP:端口（server 暂不返 IP；用 device_info 替代显示）
        val firstRuntime = group.runtimes.first()
        val endpoint = buildEndpoint(firstRuntime)
        Text(
            text = endpoint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
        // 第 3 行：provider 图标 + 数量（v0.3.19 老板 2026-06-08 需求：细化 AI 列表）
        val providers = group.runtimes.map { it.profile ?: "unknown" }.distinct().sorted()
        if (providers.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                providers.forEach { p ->
                    ProviderChip(provider = p, count = group.runtimes.count { it.profile == p })
                }
            }
        } else {
            Text(
                text = "无 provider",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // v0.3.42: 显示 runtime 版本号（来自 metadata.cli_version）
        val versions = group.runtimes.mapNotNull { it.metadata?.cliVersion }.distinct()
        if (versions.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                versions.forEach { v ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF8E8E93).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "v$v",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    } // Column 关闭
    } // Card 关闭
} // HostCard 关闭

/** server 暂不返 IP:端口；用 device_info + provider 拼一行可读文本 */
private fun buildEndpoint(rt: Runtime): String {
    val parts = mutableListOf<String>()
    rt.deviceName?.let { parts.add(it) }
    rt.profile?.let { parts.add(it) }
    if (parts.isEmpty()) parts.add("runtime://${rt.id.take(8)}")
    return parts.joinToString(" · ")
}

/**
 * v0.3.19: Provider 标签（图标 + 名称 + 数量）
 *  - 用 emoji 当图标（multica 端有：Openclaw=🦾 / Claude=🤖 / Hermes=⚡ / Opencode=📦）
 *  - 找不到对应 emoji 时降级为通用齿轮
 */
@Composable
private fun ProviderChip(provider: String, count: Int) {
    val (icon, color) = when (provider.lowercase()) {
        "openclaw" -> "🦾" to Color(0xFFFF8C00)  // 橙色
        "claude" -> "🤖" to Color(0xFFD97706)    // 棕
        "hermes" -> "⚡" to Color(0xFF22C55E)     // 绿
        "opencode" -> "📦" to Color(0xFF3B82F6)   // 蓝
        else -> "⚙" to Color(0xFFA1A1A6)         // 灰
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = icon, style = MaterialTheme.typography.labelMedium)
        Text(
            text = "${provider} ×$count",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun EmptyTab(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * v0.3.29 老板 2026-06-08 需求：runtimes 卡片中"点击状态图标可以查看该 daemon 的详细状态"
 *  - AlertDialog 形式
 *  - 列出该 host 上所有 runtime + 该 host 上所有 agent
 *  - 显示汇总状态 + 详细状态文字
 */
@Composable
private fun DaemonDetailDialog(
    group: HostGroup,
    agents: List<Agent>,
    onDismiss: () -> Unit,
) {
    val hostAgentIds = group.runtimes.map { it.id }.toSet()
    val hostAgents = agents.filter { it.runtimeId in hostAgentIds }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(state = group.aggregateState)
                Text("🖥 ${group.hostName}", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 汇总状态
                Text(
                    text = "汇总状态：${statusLabel(group.aggregateState)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                HorizontalDivider()
                // runtimes 列表
                Text("Runtimes (${group.runtimes.size})", style = MaterialTheme.typography.titleSmall)
                group.runtimes.forEach { rt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        StatusDot(state = rt.state)
                        Text("• ${rt.profile ?: "?"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("(${rt.state})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    rt.deviceName?.takeIf { it.isNotBlank() }?.let {
                        Text("  $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                    }
                }
                // agents 列表
                if (hostAgents.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Agents (${hostAgents.size})", style = MaterialTheme.typography.titleSmall)
                    hostAgents.forEach { a ->
                        Text(
                            text = "• ${a.name} (${a.state})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
