package com.multica.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multica.app.data.model.Attachment
import com.multica.app.data.model.IssueComment
import com.multica.app.data.model.IssueDetail
import com.multica.app.ui.viewmodel.DashboardViewModel

/**
 * Issue 详情页 — v0.3.5
 *
 * 来源：点 Issues Tab / Boss Tab 卡片 → vm.openIssue(issueId) → 跳到本页面
 *
 * 展示：
 *  1. Top bar：← 返回 + key + 标题
 *  2. 状态 + 优先级 + 分配 agent
 *  3. 描述（description）— Markdown 纯文本渲染
 *  4. 时间：开始 / 截止
 *  5. 标签 + 附件
 *  6. 评论列表（按时间升序）：👤 用户 / 🤖 agent 头像 + 名字 + 时间 + 内容 + 附件
 */
@Composable
fun IssueDetailScreen(
    vm: DashboardViewModel,
    agentNameOf: (String) -> String? = { null },
) {
    val s by vm.issueDetail.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.closeIssue() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color(0xFF0A84FF))
            }
            Text(
                text = s.detail?.key ?: "Issue",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF0A84FF),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = " · ${s.detail?.title.orEmpty()}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF5F5F7),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1,
            )
        }
        when {
            s.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0A84FF))
                }
            }
            s.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载失败：${s.error}", color = Color(0xFFEF4444))
                }
            }
            s.detail != null -> {
                val d = s.detail!!
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { HeaderCard(d) }
                    if (!d.description.isNullOrBlank()) {
                        item { DescriptionCard(d.description!!) }
                    }
                    if (d.attachments.isNotEmpty()) {
                        item { AttachmentsCard(d.attachments) }
                    }
                    if (s.comments.isNotEmpty()) {
                        item {
                            Text(
                                text = "💬 评论（${s.comments.size}）",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFF5F5F7),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(s.comments, key = { it.id }) { c ->
                            CommentCard(c, agentNameOf(c.authorId ?: ""))
                        }
                    } else {
                        item {
                            Text(
                                text = "暂无评论",
                                color = Color(0xFF6B6B70),
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(d: com.multica.app.data.model.IssueDetail) {
    val (statusLabel, statusColor) = statusInfo(d.status)
    val (priorityLabel, priorityColor) = priorityInfo(d.priority)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF2C2C2E), shape = RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(statusLabel, statusColor)
                PriorityPill(priorityLabel, priorityColor)
                Text(
                    text = "#${d.number}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA1A1A6),
                    fontFamily = FontFamily.Monospace,
                )
            }
            // 分配
            val whoIcon = if (d.assigneeType == "agent") "🤖" else "👤"
            val whoType = when (d.assigneeType) {
                "agent" -> "Agent"
                "member" -> "Member"
                else -> d.assigneeType ?: "未分配"
            }
            val whoId = d.assigneeId?.take(8) ?: "—"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(whoIcon, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "$whoType · $whoId",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF5F5F7),
                )
            }
            // 创建人
            val cIcon = if (d.creatorType == "agent") "🤖" else "👤"
            Text(
                text = "$cIcon 创建人 · ${d.creatorId?.take(8) ?: "—"}  ·  ${d.createdAt?.take(16) ?: "—"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA1A1A6),
            )
            // 时间
            if (d.startDate != null || d.dueDate != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFA1A1A6), modifier = Modifier.padding(0.dp))
                    Text(
                        text = "开始: ${d.startDate?.take(10) ?: "—"}  →  截止: ${d.dueDate?.take(10) ?: "—"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8E8E93),
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            // 标签
            if (d.labels.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    d.labels.take(6).forEach { lab ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2C2C2E), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = lab,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1A6),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptionCard(desc: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF2C2C2E), shape = RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF0A84FF))
                Text(
                    text = "描述",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF0A84FF),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE5E5EA),
            )
        }
    }
}

@Composable
private fun AttachmentsCard(atts: List<Attachment>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF2C2C2E), shape = RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFF9F0A))
                Text(
                    text = "附件 (${atts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFFF9F0A),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            atts.forEach { att ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📎", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = att.filename,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF5F5F7),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Text(
                        text = humanSize(att.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1A6),
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentCard(c: IssueComment, authorName: String?) {
    val isAgent = c.authorType == "agent"
    val isSystem = c.type == "system"
    val accent = when {
        isSystem -> Color(0xFF6B6B70)
        isAgent -> Color(0xFF0A84FF)
        else -> Color(0xFF22C55E)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = accent, shape = RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isAgent) "🤖" else "👤",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = authorName ?: c.authorId?.take(8) ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF5F5F7),
                    fontWeight = FontWeight.Medium,
                )
                if (isSystem) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(text = "system", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1A6))
                    }
                }
                Text(
                    text = c.createdAt?.replace("T", " ")?.take(16) ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA1A1A6),
                    modifier = Modifier.weight(1f),
                )
            }
            // Markdown 纯文本（去掉 # * [] 等符号）
            val cleanContent = c.content
                .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                .replace(Regex("\\*(.+?)\\*"), "$1")
                .replace(Regex("`([^`]+)`"), "$1")
                .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            Text(
                text = cleanContent,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE5E5EA),
            )
            if (c.attachments.isNotEmpty()) {
                c.attachments.forEach { att ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📎", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = att.filename,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0A84FF),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PriorityPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

private fun statusInfo(s: String): Pair<String, Color> = when (s.lowercase()) {
    "in_progress" -> "进行中" to Color(0xFF0A84FF)
    "in_review" -> "审核中" to Color(0xFFA855F7)
    "blocked" -> "阻塞" to Color(0xFFEF4444)
    "todo" -> "待办" to Color(0xFF6B6B70)
    "backlog" -> "规划" to Color(0xFF6B6B70)
    "done" -> "done" to Color(0xFF22C55E)
    "cancelled" -> "已取消" to Color(0xFF6B6B70)
    else -> s to Color(0xFF6B6B70)
}

private fun priorityInfo(p: String?): Pair<String, Color> = when (p?.lowercase()) {
    "urgent" -> "紧急" to Color(0xFFEF4444)
    "high" -> "高" to Color(0xFFFF9F0A)
    "medium" -> "中" to Color(0xFFFFD60A)
    "low" -> "低" to Color(0xFF8E8E93)
    else -> "无" to Color(0xFF8E8E93)
}

private fun humanSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    b < 1024L * 1024 * 1024 -> "${b / 1024 / 1024} MB"
    else -> "${b / 1024 / 1024 / 1024} GB"
}
