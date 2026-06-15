package com.multica.app.ui.dashboard.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.multica.app.data.model.Agent
import com.multica.app.data.model.Issue
import com.multica.app.data.model.Runtime
import com.multica.app.ui.components.StatusDot
import com.multica.app.ui.components.Avatar
import com.multica.app.ui.viewmodel.DashboardUiState

/**
 * Agents Tab — 2026-06-08 老板要求版本
 *
 * 视觉：
 *  - 一行 2 个 agent
 *  - 卡片**统一灰色背景**（不再分忙/闲/离线三色）
 *  - 状态圆点 4 档（绿/红/黄/灰）— agent 状态（无 task 信息时） / runtime 状态（有 task 信息时）
 *  - **工作中时**卡片边框**蓝色闪烁**（用 `infiniteRepeatable` 动画）
 *
 * 内容 3 行：
 *  1. agent 名称 + 状态圆点
 *  2. 工作状态 / 当前的 task（busy 时显示 "工作中: JIMI-xxx"，idle 时显示 "空闲"）
 *  3. 上一次工作时间（updated_at → "x 分钟前"）
 *
 * 排序：工作中 > 空闲 > 离线（按 updated_at 新的在前）
 */
@Composable
fun AgentsTab(
    s: DashboardUiState,
    runtimes: List<Runtime> = emptyList(),
    colsPortrait: Int = 2,
    colsLandscape: Int = 3,
) {
    if (s.agents.isEmpty()) {
        EmptyTab(text = "工作区里还没有 agent — 在 Web 里 Settings → Agents 新建")
        return
    }
    val rtMap = remember(runtimes) { runtimes.associateBy { it.id } }
    // v0.3.42: 负载使用 agent tasks API 的真实 running task 数
    // 待办仍从 issues 的 todo 状态计算
    val loadByAgent = remember(s.agentRunningTasks, s.issues, s.agents) {
        val nameToId = s.agents.associate { it.name to it.id }
        val agentIds = s.agents.map { it.id }.toSet()
        fun resolveAgentId(issue: com.multica.app.data.model.Issue): String? {
            if (issue.assigneeId != null && issue.assigneeId in agentIds &&
                (issue.isAgentAssignee || issue.assigneeType == "agent")) return issue.assigneeId
            if (issue.creatorType == "agent" && issue.creatorId in agentIds) return issue.creatorId
            if (issue.assigneeName != null) return nameToId[issue.assigneeName]
            return null
        }
        val todoByAgent = s.issues.filter { it.status == "todo" }
            .groupBy { resolveAgentId(it) ?: "_unknown" }
        s.agents.associate { agent ->
            val running = s.agentRunningTasks[agent.id] ?: 0
            val todo = todoByAgent[agent.id]?.size ?: 0
            agent.id to (running to todo)  // (负载=running tasks, 待办=todo issues)
        }
    }
    // v0.3.15: 取消 in_progress / in_review 推算 — 改用 server 真实状态
    val sorted = remember(s.agents, rtMap, loadByAgent) {
        s.agents.sortedWith(compareBy(
            { agentStatePriority(it, rtMap, loadByAgent[it.id]?.first ?: 0, loadByAgent[it.id]?.second ?: 0) },
            { -it.minuteBucketSortKey },  // v0.3.12: 60秒 bucket 稳定排序
            { it.name },
        ))
    }
    // v0.3.14: 自适应高度 — 算每行卡应得高度（屏高 / 行数）
    // v0.3.41: 列数可配置（竖屏/横屏），默认竖屏2横屏3
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cols = if (maxWidth > 600.dp) colsLandscape.coerceIn(1, 4) else colsPortrait.coerceIn(1, 4)
        val rows = (sorted.size + cols - 1) / cols
        val paddingV = 16.dp
        val rowGap = 8.dp
        val perRowH = if (rows > 0) {
            (maxHeight - paddingV - rowGap * (rows - 1).coerceAtLeast(0)) / rows
        } else 0.dp

    // v0.3.41: 排序变化时自动滚回顶部，避免卡片上移后页面停留在原位
    val gridState = rememberLazyGridState()
    val sortedIds = sorted.map { it.id }
    LaunchedEffect(sortedIds) {
        gridState.scrollToItem(0)
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(cols),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(sorted, key = { it.id }, span = { GridItemSpan(1) }) @OptIn(ExperimentalFoundationApi::class) { a ->
                // 响应式排序动画：v0.3.8 新增 — 卡片位置变化时 350ms 渐变过渡
                AgentCard(
                    a = a,
                    runtime = rtMap[a.runtimeId],
                    minHeight = perRowH,  // v0.3.14: 自适应高度
                    currentLoad = loadByAgent[a.id]?.first ?: 0,   // 负载（in_progress+in_review，busy时至少1）
                    todoCount = loadByAgent[a.id]?.second ?: 0,    // 待办（todo数）
                    agentIssues = s.issues.filter { issue ->
                        resolveAgentIdForIssue(issue, s.agents) == a.id &&
                            issue.status in listOf("in_progress", "todo", "in_review")
                    },
                    modifier = Modifier.animateItemPlacement(animationSpec = tween(350)),
                )
            }
    }
    } // BoxWithConstraints 关闭
}

/** 供 AgentCard 使用的全局匹配函数 */
private fun resolveAgentIdForIssue(issue: Issue, agents: List<Agent>): String? {
    val nameToId = agents.associate { it.name to it.id }
    val agentIds = agents.map { it.id }.toSet()
    if (issue.assigneeId != null && issue.assigneeId in agentIds &&
        (issue.isAgentAssignee || issue.assigneeType == "agent")) return issue.assigneeId
    if (issue.creatorType == "agent" && issue.creatorId in agentIds) return issue.creatorId
    if (issue.assigneeName != null) return nameToId[issue.assigneeName]
    return null
}

@Composable
private fun AgentCard(
    a: Agent,
    runtime: Runtime?,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    currentLoad: Int = 0,   // in_progress 数（负载）
    todoCount: Int = 0,     // todo 数（待办）
    agentIssues: List<Issue> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var showDetail by remember { mutableStateOf(false) }
    val rtOffline = runtime != null && !isOnline(runtime.state)
    val busy = isBusy(a.state, a.updatedAt)  // v0.3.15: 取消推算，仅 server 状态 + 30s 兜底
    // === 工作中：border 蓝闪（v0.3.8 强化） ===
    // - LaunchedEffect 持续在 isBusy=true 时翻转 alpha（800ms 周期）
    // - 用 animateColorAsState 让 borderColor 渐变（idle→busy 时不会突然跳变）
    var flashOn by remember(busy) { mutableStateOf(busy) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (flashOn) 1.0f else 0.3f,
        animationSpec = tween(durationMillis = 800),
        label = "border-flash",
    )
    LaunchedEffect(busy) {
        if (busy) {
            while (true) {
                flashOn = true
                delay(800)
                flashOn = false
                delay(800)
            }
        } else {
            flashOn = false
        }
    }
    val borderColor by animateColorAsState(
        targetValue = if (busy) Color(0xFF0A84FF).copy(alpha = if (flashOn) 1.0f else 0.25f)
                      else Color(0xFF2C2C2E),
        animationSpec = tween(300),
        label = "border-color",
    )
    val borderWidth = if (busy) 2.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)  // v0.3.14: 自适应高度（屏高/行数）
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(10.dp))
            .clickable { showDetail = true },
        shape = RoundedCornerShape(10.dp),
        // 老板要求：卡片以**灰色**展示 — 用 surfaceVariant（iOS 灰 #1C1C1E）
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 第 1 行：agent 名称 + 状态圆点
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // v0.3.14 圆点状态：表示 **runtime 是否在线**（不是 agent 是否在工作）
                //  - runtime 在线 → 绿
                //  - runtime 离线 → 红
                //  - 异常 / 启动中 → 黄
                //  agent 状态（busy/idle）由第 2 行文字 + 边框闪烁表达，**不**用圆点
                val dotState = when {
                    runtime == null -> "unknown"
                    rtOffline -> "offline"
                    else -> "online"
                }
                StatusDot(state = dotState, modifier = Modifier.padding(1.dp))
                Text(
                    text = a.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF5F5F7),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
            // v0.3.41: 状态文字反映"当前负载"
            val workText = when {
                rtOffline -> "runtime 离线"
                busy -> "工作中"
                todoCount > 0 && a.state == "idle" -> "待办"
                a.state == "idle" -> "空闲"
                a.state == "offline" -> "离线"
                else -> a.state
            }
            val workColor = when {
                busy -> Color(0xFF0A84FF)  // 蓝（工作中）
                todoCount > 0 && a.state == "idle" -> Color(0xFFFF9F0A) // 橙（有待办）
                rtOffline -> Color(0xFFEF4444) // 红
                else -> Color(0xFFA1A1A6)
            }
            // 负载矩形颜色
            val hasLoad = currentLoad > 0 || todoCount > 0
            val loadBgColor = if (hasLoad) Color(0xFF0A84FF) else Color(0xFF3A3A3C)
            val loadTextColor = if (hasLoad) Color.White else Color(0xFF8E8E93)
            // 第 2 行：头像 + 状态文字 + 负载
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左侧：头像 + 状态文字
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (!a.avatarUrl.isNullOrBlank()) {
                        Avatar(
                            url = a.avatarUrl,
                            size = 48.dp,
                            fallback = a.name.firstOrNull()?.toString() ?: "?",
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3A3A3C)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = a.name.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF5F5F7),
                            )
                        }
                    }
                    Text(
                        text = workText,
                        style = MaterialTheme.typography.labelMedium,
                        color = workColor,
                        fontWeight = if (busy) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                // 右侧：负载矩形
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(loadBgColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$currentLoad/$todoCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = loadTextColor,
                    )
                }
            }
            // 第 3 行：模型名称（独立一行，不与状态文字同行）
            val modelText = a.model?.takeIf { it.isNotBlank() } ?: "default"
            Text(
                text = modelText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            // v0.3.15 第 3 行：**上次任务时间**（v0.3.13 加的完整年月日时分秒）
            val lastTime = formatFullDateTime(a.updatedAt)
            Text(
                text = lastTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93),
                fontFamily = FontFamily.Monospace,
            )
        }
    }
    // v0.3.42: 点击卡片弹出详情
    if (showDetail) {
        AgentDetailDialog(
            agent = a,
            runtime = runtime,
            issues = agentIssues,
            onDismiss = { showDetail = false },
        )
    }
}

private fun isOnline(state: String): Boolean = when (state.lowercase()) {
    "online", "running", "connected", "active", "starting" -> true
    else -> false
}

/**
 * 判断 agent 是否在工作中（老板 2026-06-08 需求：工作中卡片蓝边框闪烁）
 *  v0.3.10 收紧判定（之前 5 分钟 updated_at 窗口太宽，已停止工作的 agent 还显示工作中）：
 *  1. agent.status 字段 = busy / in_progress / working / running / active
 *  2. （推算）该 agent 被分配到 in_progress 状态 issue → 真在执行任务
 *  3. （推算）该 agent 被分配到 in_review 状态 issue → 审核中（视为工作中）
 *  4. （推算）该 agent 在最近 **30 秒** 内有 updated_at（活动迹象，窗口短才准确）
 */
private fun isBusy(state: String, updatedAt: String? = null, assignedInProgress: Boolean = false, assignedInReview: Boolean = false): Boolean {
    if (state.lowercase() in setOf("busy", "in_progress", "working", "running", "active")) return true
    if (assignedInProgress) return true      // in_progress issue = 真在工作
    if (assignedInReview) return true        // in_review issue = 审核中
    return try {
        val odt = java.time.OffsetDateTime.parse(updatedAt ?: return false)
        val secs = java.time.Duration.between(odt, java.time.OffsetDateTime.now()).seconds
        secs in 0..30
    } catch (_: Throwable) {
        false
    }
}

/** 排序优先级（v0.3.40 更新：加入"待办"状态）：
 *  1. 工作中（agent.status = busy）— 最前
 *  2. 待办（idle 但有 todo/in_progress 任务）— 次前
 *  3. 空闲（idle 且无任务，runtime 在线）
 *  4. 离线（runtime 离线 或 agent 状态为 offline）
 *  5. 其它未知 — 最后
 */
private fun agentStatePriority(a: Agent, rtMap: Map<String, Runtime>, currentLoad: Int = 0, todoCount: Int = 0): Int {
    val rt = rtMap[a.runtimeId]
    val rtOffline = rt != null && !isOnline(rt.state)
    val busy = isBusy(a.state, a.updatedAt)
    return when {
        busy -> 0                                      // 工作中 → 最前
        a.state == "idle" && (currentLoad > 0 || todoCount > 0) && !rtOffline -> 1  // 待办
        a.state == "idle" && !rtOffline -> 2           // 空闲 + runtime 在线
        rtOffline || a.state == "offline" -> 3         // 离线
        else -> 4                                       // 未知
    }
}

/** 简单把 ISO 时间转成 "x 分钟前" / "x 小时前"（不依赖具体时区） */
private fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        // 格式: 2026-06-08T01:37:20Z
        val (date, _) = iso.split("T", limit = 2)
        val (y, m, d) = date.split("-").map { it.toInt() }
        val now = java.time.LocalDate.now()
        val dt = java.time.LocalDate.of(y, m, d)
        val days = java.time.temporal.ChronoUnit.DAYS.between(dt, now)
        when {
            days < 0 -> date  // 未来
            days == 0L -> "今天"
            days == 1L -> "昨天"
            days < 7L -> "${days} 天前"
            days < 30L -> "${days / 7} 周前"
            else -> "${days / 30} 月前"
        }
    } catch (_: Throwable) {
        iso.take(10)
    }
}

/** Agent.updatedAt 字符串转 long 用于排序（按秒）。
 *  注意：v0.3.12 改用 `minuteBucketSortKey`，避免 server 每次 polling 推新事件导致 updated_at
 *  抖动（毫秒级变化）造成排序来回变。
 */
private val Agent.updatedAtSortKey: Long
    get() = try {
        java.time.OffsetDateTime.parse(updatedAt ?: "1970-01-01T00:00:00Z").toEpochSecond()
    } catch (_: Throwable) {
        0L
    }

/** v0.3.12 新增：60 秒 bucket 排序键。
 *  同一分钟内所有 agent 拥有相同 bucket，排序稳定；只在新一分钟到来时才会让"刚刚活跃的"往上走。
 *  这样避免 server 每次 push 1ms 抖动都让排序位置来回变。
 */
private val Agent.minuteBucketSortKey: Long
    get() = try {
        java.time.OffsetDateTime.parse(updatedAt ?: "1970-01-01T00:00:00Z").toEpochSecond() / 60
    } catch (_: Throwable) {
        0L
    }

/** v0.3.13 新增：agent 更新时间显示**年月日时分秒**（老板 2026-06-08 需求）。
 *  格式：2026-06-08 14:55:37（系统时区）。
 *  server 返 ISO（UTC），用 Instant.parse + ZoneId.systemDefault() 转换成本地时间。
 */
private fun formatFullDateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val instant = java.time.Instant.parse(iso)
        val local = instant.atZone(java.time.ZoneId.systemDefault())
        val f = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        local.format(f)
    } catch (_: Throwable) {
        iso.take(19).replace("T", " ")
    }
}

/** v0.3.42: Agent 详情弹窗 — 显示名称、runtime、模型、任务列表 */
@Composable
private fun AgentDetailDialog(
    agent: Agent,
    runtime: Runtime?,
    issues: List<Issue>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1E),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 标题行：名称 + 关闭
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = agent.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5F5F7),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp),
                    )
                }

                // Runtime 信息
                val rtName = runtime?.name ?: "—"
                val rtState = runtime?.state ?: "—"
                val rtOnline = runtime != null && isOnline(runtime.state)
                DetailRow("Runtime", rtName)
                DetailRow("状态", if (rtOnline) "在线" else rtState)

                // 当前模型
                val model = agent.model?.takeIf { it.isNotBlank() } ?: "—"
                DetailRow("模型", model)

                // 运行模式
                val mode = agent.mode ?: "—"
                DetailRow("模式", mode)

                // 最大并发
                val maxConcurrent = agent.maxConcurrentTasks?.toString() ?: "—"
                DetailRow("最大并发", maxConcurrent)

                // 分隔线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF3A3A3C))
                )

                // 任务列表
                val inProgress = issues.filter { it.status == "in_progress" }
                val inReview = issues.filter { it.status == "in_review" }
                val todo = issues.filter { it.status == "todo" }

                Text(
                    text = "任务情况",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF5F5F7),
                )

                // 进行中
                TaskSection("进行中 (${inProgress.size})", inProgress, Color(0xFF0A84FF))
                // 审批中
                TaskSection("审批中 (${inReview.size})", inReview, Color(0xFFFF9F0A))
                // 待办
                TaskSection("待办 (${todo.size})", todo, Color(0xFF8E8E93))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8E8E93),
            modifier = Modifier.width(60.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFF5F5F7),
        )
    }
}

@Composable
private fun TaskSection(title: String, issues: List<Issue>, accentColor: Color) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
        )
        if (issues.isEmpty()) {
            Text(
                text = "  无",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF636366),
            )
        } else {
            issues.forEach { issue ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${issue.key}  ${issue.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD1D1D6),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
