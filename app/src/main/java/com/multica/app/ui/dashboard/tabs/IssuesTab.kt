package com.multica.app.ui.dashboard.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multica.app.data.model.Agent
import com.multica.app.data.model.Issue
import com.multica.app.ui.viewmodel.DashboardUiState
import kotlinx.coroutines.delay

/**
 * Issues Tab — 2026-06-08 老板要求版本
 *
 * 视觉：
 *  - 一行 1 个 issue
 *  - 卡片**统一灰色背景**
 *  - 工作中（in_progress）→ 边框**蓝色闪烁**
 *  - 阻塞（blocked）→ 边框**红色闪烁**
 *  - 其它状态：固定灰色边框
 *
 * 内容 3 行：
 *  1. issue 名（key + 标题）+ 状态文字
 *  2. 所属项目 · 分配的 agent
 *  3. 开始时间 + 截止时间
 *
 * 排序：
 *  - 优先按优先级：urgent > high > medium > low > null
 *  - 其次按状态：blocked > in_review > in_progress > todo > backlog > done
 */
@Composable
fun IssuesTab(
    s: DashboardUiState,
    agents: List<Agent> = emptyList(),
    onIssueClick: (String) -> Unit = {},
) {
    if (s.issues.isEmpty()) {
        EmptyTab(text = "工作区里还没有 issue — 在 Web 或 CLI 创建")
        return
    }
    val agentMap = remember(agents) { agents.associate { it.id to it.name } }
    // v0.3.18: 老板 2026-06-08 需求 — 排序：阻塞>审核>待办>规划，done 隐藏
    val sorted = remember(s.issues, agents) {
        s.issues
            .filter { it.status !in setOf("done", "cancelled") }  // 隐藏 done/cancelled
            .sortedWith(
                compareBy(
                    { statusRank(it.status) },  // 状态优先级（主）
                    { priorityRank(it.priority) },
                    { -it.dueDateSortKey },
                )
            )
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(sorted, key = { it.id }) { issue ->
            IssueCard(issue, agentName = issue.assigneeId?.let { agentMap[it] })
        }
    }
}

@Composable
private fun IssueCard(it: Issue, agentName: String?, onClick: () -> Unit = {}) {
    val inProgress = it.status == "in_progress"
    val blocked = it.status == "blocked"
    // 工作中蓝闪；阻塞红闪
    var flashAlpha by remember(inProgress, blocked) {
        mutableFloatStateOf(if (inProgress || blocked) 1.0f else 1.0f)
    }
    LaunchedEffect(inProgress, blocked) {
        if (inProgress) {
            while (true) {
                flashAlpha = 1.0f; delay(800); flashAlpha = 0.3f; delay(800)
            }
        } else if (blocked) {
            while (true) {
                flashAlpha = 1.0f; delay(600); flashAlpha = 0.2f; delay(600)
            }
        } else {
            flashAlpha = 1.0f
        }
    }
    val borderColor = when {
        inProgress -> Color(0xFF0A84FF).copy(alpha = flashAlpha)  // 工作中蓝闪
        blocked -> Color(0xFFEF4444).copy(alpha = flashAlpha)      // 阻塞红闪
        else -> Color(0xFF2C2C2E)
    }
    val borderWidth = if (inProgress || blocked) 2.dp else 1.dp
    val (statusLabel, statusColor) = statusInfo(it.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        // 老板要求：卡片以**灰色**展示
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        onClick = onClick,
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 第 1 行：key + 标题 + 状态文字
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = it.key,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF0A84FF),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = it.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF5F5F7),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                )
            }
            // 第 2 行：所属项目 + 分配的 agent
            val project = it.projectId?.takeLast(8) ?: "—"
            val who = when {
                it.assigneeName != null -> it.assigneeName
                it.assigneeId != null -> it.assigneeId.take(8)
                else -> "未分配"
            }
            val whoIcon = if (it.isAgentAssignee) "🤖" else "👤"
            Text(
                text = "项目: $project  ·  $whoIcon $who",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA1A1A6),
            )
            // 第 3 行：开始时间 + 截止时间
            val start = it.startDate?.let { d -> d.take(10) } ?: "—"
            val due = it.dueDate?.let { d -> d.take(10) } ?: "—"
            val dueColor = when {
                it.dueDate == null -> Color(0xFFA1A1A6)
                isOverdue(it.dueDate) -> Color(0xFFEF4444)
                else -> Color(0xFF8E8E93)
            }
            Text(
                text = "开始: $start  →  截止: $due",
                style = MaterialTheme.typography.labelSmall,
                color = dueColor,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/** 优先级排序：urgent 最小（靠前），null 最大（靠后） */
private fun priorityRank(p: String?): Int = when (p?.lowercase()) {
    "urgent" -> 0
    "high" -> 1
    "medium" -> 2
    "low" -> 3
    else -> 4
}

/** 状态排序（v0.3.18 老板 2026-06-08 需求）：
 *   阻塞 > 审核中 > 进行中 > 待办 > 规划
 *   done / cancelled 不显示（在 filter 里过滤）
 */
private fun statusRank(s: String): Int = when (s.lowercase()) {
    "blocked" -> 0      // 阻塞最前
    "in_review" -> 1    // 审核中
    "in_progress" -> 2  // 进行中（在审核后、待办前 — 进行中是 hot work）
    "todo" -> 3         // 待办
    "backlog" -> 4      // 规划
    else -> 99          // done/cancelled 已过滤，到这里不可能
}

private fun statusInfo(s: String): Pair<String, Color> = when (s.lowercase()) {
    "in_progress" -> "进行中" to Color(0xFF0A84FF)
    "in_review" -> "审核中" to Color(0xFFA855F7)
    "blocked" -> "阻塞" to Color(0xFFEF4444)
    "todo" -> "待办" to Color(0xFFA1A1A6)
    "backlog" -> "规划" to Color(0xFF6B6B70)
    "done" -> "done" to Color(0xFF22C55E)
    "cancelled" -> "已取消" to Color(0xFF6B6B70)
    else -> s to Color(0xFFA1A1A6)
}

/** 截止日期 < 今天 → 红色 */
private fun isOverdue(dueDate: String): Boolean = try {
    val (y, m, d) = dueDate.take(10).split("-").map { it.toInt() }
    val dt = java.time.LocalDate.of(y, m, d)
    dt.isBefore(java.time.LocalDate.now())
} catch (_: Throwable) {
    false
}

/** Issue.dueDate 字符串转 long 用于排序（无 dueDate 的排最后） */
private val Issue.dueDateSortKey: Long
    get() = try {
        java.time.LocalDate.parse(dueDate?.take(10) ?: "9999-12-31")
            .toEpochDay()
    } catch (_: Throwable) {
        Long.MAX_VALUE
    }
