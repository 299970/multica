package com.multica.app.ui.dashboard.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multica.app.data.model.InboxItem
import com.multica.app.data.model.Issue
import com.multica.app.ui.viewmodel.DashboardUiState

/**
 * Boss Tab — 2026-06-08 老板新增需求
 *
 * 老板的需求 4 段（docs/requirements.md line 78-92）：
 *  1. @will 的（assignee = 当前用户）
 *  2. 需要审核的（status = in_review）
 *  3. 有 chat 消息的（inbox type = new_comment / chat）
 *  4. （隐含）owner 的 inbox 全部
 *
 * 实现：
 *  - 4 个子分组，**竖排卡片**（每组一段小标题 + 卡片列表）
 *  - 灰底，状态色边
 *  - 显示未读小红点
 */
@Composable
fun BossTab(
    s: DashboardUiState,
    onRefresh: () -> Unit = {},
    onIssueClick: (String) -> Unit = {},
) {
    if (s.bossInbox.isEmpty() && s.bossMyIssues.isEmpty() && s.bossReviewIssues.isEmpty() && s.bossChatMessages.isEmpty()) {
        EmptyTab(text = "没有 Boss 待办 — 所有事都处理完了 ✨")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (s.bossMyIssues.isNotEmpty()) {
            item { SectionHeader("👤 @will（分配给我的）", s.bossMyIssues.size) }
            items(s.bossMyIssues, key = { "my-${it.id}" }) { issue ->
                BossIssueCard(issue, color = Color(0xFF8B5CF6))
            }
        }
        if (s.bossReviewIssues.isNotEmpty()) {
            item { SectionHeader("🔍 需要审核（in_review）", s.bossReviewIssues.size) }
            items(s.bossReviewIssues, key = { "rv-${it.id}" }) { issue ->
                BossIssueCard(issue, color = Color(0xFFA855F7))
            }
        }
        if (s.bossChatMessages.isNotEmpty()) {
            item { SectionHeader("💬 Chat 消息", s.bossChatMessages.size) }
            items(s.bossChatMessages, key = { "c-${it.id}" }) { msg ->
                BossInboxCard(msg)
            }
        }
        val otherInbox = s.bossInbox.filter {
            !it.isChat && it.issueId == null
        }
        if (otherInbox.isNotEmpty()) {
            item { SectionHeader("📨 Inbox 通知", otherInbox.size) }
            items(otherInbox, key = { "i-${it.id}" }) { msg ->
                BossInboxCard(msg)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFF5F5F7),
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .background(Color(0xFF3A3A3C), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA1A1A6),
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun BossIssueCard(issue: Issue, color: Color, onClick: () -> Unit = {}) {
    val (statusLabel, statusColor) = statusInfo(issue.status)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // 第 1 行：key + 标题 + 状态
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = issue.key,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = issue.title,
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
            // 第 2 行：分配的 agent
            val who = issue.assigneeName ?: issue.assigneeId?.take(8) ?: "未分配"
            val icon = if (issue.isAgentAssignee) "🤖" else "👤"
            Text(
                text = "$icon $who",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA1A1A6),
            )
            // 第 3 行：截止时间
            val due = issue.dueDate?.take(10) ?: "—"
            val dueColor = when {
                issue.dueDate == null -> Color(0xFFA1A1A6)
                isOverdue(issue.dueDate) -> Color(0xFFEF4444)
                else -> Color(0xFF8E8E93)
            }
            Text(
                text = "截止: $due",
                style = MaterialTheme.typography.labelSmall,
                color = dueColor,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun BossInboxCard(msg: InboxItem) {
    val accent = when (msg.severity?.lowercase()) {
        "warning" -> Color(0xFFFF9F0A)
        "error" -> Color(0xFFEF4444)
        "success" -> Color(0xFF22C55E)
        else -> Color(0xFF0A84FF)
    }
    val unread = !msg.read
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (unread) 2.dp else 1.dp, color = accent.copy(alpha = if (unread) 1f else 0.4f), shape = RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (unread) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = msg.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF5F5F7),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    text = relativeTime(msg.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA1A1A6),
                )
            }
            if (!msg.body.isNullOrBlank()) {
                Text(
                    text = msg.body,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA1A1A6),
                    maxLines = 2,
                )
            }
            // 第 3 行：类型 + 来源
            val from = msg.actorType?.let { "${it.takeLast(4)} #${msg.actorId?.take(8) ?: ""}" } ?: ""
            Text(
                text = "${typeLabel(msg.type)}  ·  $from",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private fun typeLabel(t: String): String = when (t.lowercase()) {
    "new_comment" -> "💬 新评论"
    "mention" -> "@ 提及"
    "chat" -> "💬 Chat"
    "task_assigned" -> "📋 任务分配"
    "issue_status" -> "🔄 状态变化"
    "new_issue" -> "📝 新 issue"
    else -> t
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

private fun isOverdue(dueDate: String): Boolean = try {
    val (y, m, d) = dueDate.take(10).split("-").map { it.toInt() }
    java.time.LocalDate.of(y, m, d).isBefore(java.time.LocalDate.now())
} catch (_: Throwable) {
    false
}

private fun relativeTime(iso: String?): String = try {
    val odt = java.time.OffsetDateTime.parse(iso)
    val now = java.time.OffsetDateTime.now()
    val secs = java.time.Duration.between(odt, now).seconds
    when {
        secs < 60 -> "刚刚"
        secs < 3600 -> "${secs / 60} 分钟前"
        secs < 86400 -> "${secs / 3600} 小时前"
        secs < 604800 -> "${secs / 86400} 天前"
        else -> odt.toLocalDate().toString()
    }
} catch (_: Throwable) {
    iso?.take(10) ?: "—"
}
