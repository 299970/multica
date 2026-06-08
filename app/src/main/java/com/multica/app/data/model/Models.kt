package com.multica.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ============================================================
 *  Multica 数据模型
 *  - 字段命名遵循服务器实际返回的 snake_case (部分驼峰兼容)
 *  - 所有 optional 字段用可空类型 + 默认值
 *  - 解析失败必须回退到安全默认值，绝不让 UI 白屏
 * ============================================================ */

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class Workspace(
    val id: String = "",
    val name: String = "",
    val slug: String? = null,
    @SerialName("issue_prefix") val issuePrefix: String? = null,
    val description: String? = null,
    val context: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("member_count") val memberCount: Int? = null,
)

/** Runtime = 一台机器 / 一个 daemon 注册的实体
 *  server 真实字段（`/api/runtimes`）：id, workspace_id, daemon_id, name,
 *    runtime_mode, provider, launch_header, status, device_info, last_seen_at,
 *    created_at, updated_at, metadata.{cli_version, version, launched_by}
 */
@Serializable
data class Runtime(
    val id: String = "",
    val name: String = "",
    /** "local" | "cloud" — 来自 server `runtime_mode` */
    @SerialName("runtime_mode") val type: String = "local",
    /** "online" | "offline" | ... — 来自 server `status` */
    @SerialName("status") val state: String = "unknown",
    val agents: List<String> = emptyList(),
    /** 来自 server `device_info`（"mac · OpenClaw 2026.6.1"） */
    @SerialName("device_info") val deviceName: String? = null,
    /** 来自 server `provider`（"openclaw" / "claude" / "hermes"） */
    @SerialName("provider") val profile: String? = null,
    @SerialName("last_seen_at") val lastHeartbeatAt: String? = null,
    /** 从 metadata.version + last_seen_at 算 "up since x, 5m ago" */
    val uptime: String? = null,
)

/** Agent = 调度平台里的虚拟同事
 *  server 真实字段（`/api/agents`）：id, workspace_id, runtime_id, name,
 *    description, instructions, avatar_url, runtime_mode, status, visibility,
 *    max_concurrent_tasks, max_queued_tasks
 */
@Serializable
data class Agent(
    val id: String = "",
    val name: String = "",
    val provider: String? = null, // 推算自 runtime 列表（server 单条 agent 不带）
    @SerialName("runtime_id") val runtimeId: String? = null,
    @SerialName("runtime_mode") val mode: String? = null, // "local" / "cloud"
    /** 来自 server `status` */
    @SerialName("status") val state: String = "idle", // "idle" | "busy" | ...
    val description: String? = null,
    val instructions: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val visibility: String? = null, // "workspace" | "private"
    @SerialName("max_concurrent_tasks") val maxConcurrentTasks: Int? = null,
    /** 来自 server `updated_at`（ISO 8601） */
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** Issue = 工作项
 *  server 真实字段（`/api/issues` → `{"issues": [...]}`）：
 *    id, workspace_id, number, identifier（"JIMI-123"）, title, description,
 *    status, priority, assignee_id, created_at, updated_at, closed_at,
 *    parent_issue_id, project_id, labels, ...
 */
@Serializable
data class Issue(
    val id: String = "",
    /** 来自 server `identifier`（如 "JIMI-123"） */
    @SerialName("identifier") val key: String = "",
    val title: String = "",
    val status: String = "backlog", // backlog | todo | in_progress | in_review | done | blocked | cancelled
    val priority: String? = null, // urgent | high | medium | low | null
    @SerialName("assignee_id") val assigneeId: String? = null,
    @SerialName("assignee_type") val assigneeType: String? = null, // "user" | "agent"
    val assigneeName: String? = null,
    @SerialName("is_agent_assignee") val isAgentAssignee: Boolean = false,
    val description: String? = null,
    val number: Int? = null,
    @SerialName("start_date") val startDate: String? = null,     // ISO date
    @SerialName("due_date") val dueDate: String? = null,         // ISO date
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    val labels: List<String> = emptyList(),
    val url: String? = null,
)

/** Daemon 状态（合并 Runtime + agent CLIs 列表） */
@Serializable
data class DaemonStatus(
    val state: String = "stopped", // running | stopped | starting | stopping | installing_cli | cli_not_found
    val pid: Int? = null,
    val uptime: String? = null,
    val daemonId: String? = null,
    val deviceName: String? = null,
    val agents: List<String> = emptyList(),
    val workspaceCount: Int? = null,
    val profile: String? = null,
    val serverUrl: String? = null,
)

/* ===================  简单分页响应  =================== */

@Serializable
data class PagedResponse<T>(
    val items: List<T> = emptyList(),
    val total: Int? = null,
    val nextCursor: String? = null,
)

/* ===================  Issue 详情（含 description + attachments）  =================== */
@Serializable
data class IssueDetail(
    val id: String = "",
    @SerialName("identifier") val key: String = "",
    val number: Int = 0,
    val title: String = "",
    val description: String? = null,
    val status: String = "backlog",
    val priority: String? = null,
    @SerialName("assignee_id") val assigneeId: String? = null,
    @SerialName("assignee_type") val assigneeType: String? = null,
    @SerialName("creator_id") val creatorId: String? = null,
    @SerialName("creator_type") val creatorType: String? = null,
    @SerialName("parent_issue_id") val parentIssueId: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val labels: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
)

@Serializable
data class Attachment(
    val id: String = "",
    val filename: String = "",
    val url: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

/** 来自 server `/api/issues/{id}/comments` 返回裸数组 */
@Serializable
data class IssueComment(
    val id: String = "",
    @SerialName("issue_id") val issueId: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("author_type") val authorType: String? = null,   // "member" | "agent"
    val content: String = "",
    val type: String? = "comment", // "comment" | "system" | "resolved"
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val attachments: List<Attachment> = emptyList(),
)

/* ===================  Inbox 通知  =================== */
/** server `/api/inbox` 返回裸数组
 *  字段：id, recipient_id, recipient_type, actor_id, actor_type, type, severity,
 *        issue_id, task_id, comment_id, title, body, read, created_at ...
 */
@Serializable
data class InboxItem(
    val id: String = "",
    @SerialName("recipient_id") val recipientId: String? = null,
    @SerialName("recipient_type") val recipientType: String? = null, // "member"
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("actor_type") val actorType: String? = null,         // "user" | "agent"
    val type: String = "",  // "new_comment" | "new_issue" | "task_assigned" | "mention" | "issue_status" | "chat"
    val severity: String? = "info",
    @SerialName("issue_id") val issueId: String? = null,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("comment_id") val commentId: String? = null,
    @SerialName("chat_session_id") val chatSessionId: String? = null,
    val title: String = "",
    val body: String? = null,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
) {
    /** 是否与 chat 消息有关 */
    /** v0.3.16: 只认 type=="chat" 或 chat_session_id != null（真 chat 消息）
     *  不再把 "new_comment"（任务评论）算作 chat — 评论是 issue 详情页的事
     *  老板 2026-06-08 反馈：boss tab 显示了 576 条 new_comment 当作"Chat 消息"
     */
    val isChat: Boolean get() = type == "chat" || !chatSessionId.isNullOrBlank()
    /** 是否 @ 当前用户（mentioned） */
    val isMention: Boolean get() = type == "mention" || body?.contains("@will", ignoreCase = true) == true
    /** 是否需要审核（in_review） */
    val needsReview: Boolean get() = type == "issue_status" || type == "task_assigned" && body?.contains("review", ignoreCase = true) == true
}

/* ===================  multica 真实 list 响应（带 wrap） =================== */

/** `/api/agents` → `{"agents":[...]}` */
@Serializable
data class AgentListResponse(val agents: List<Agent> = emptyList())

/** `/api/issues` → `{"issues":[...]}` */
@Serializable
data class IssueListResponse(val issues: List<Issue> = emptyList())

/** `/api/runtimes` → `{"runtimes":[...]}` */
@Serializable
data class RuntimeListResponse(val runtimes: List<Runtime> = emptyList())
