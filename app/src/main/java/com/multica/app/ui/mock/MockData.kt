package com.multica.app.ui.mock

import com.multica.app.data.model.Agent
import com.multica.app.data.model.DaemonStatus
import com.multica.app.data.model.Issue
import com.multica.app.data.model.Runtime
import com.multica.app.data.model.User
import com.multica.app.data.model.Workspace

/**
 * 未配置服务器 / 拉取失败时使用的示例数据。
 * 让 APP 即使在空状态下也能展示完整 UI 框架。
 */
object MockData {
    val user = User(
        id = "u_demo",
        name = "Demo User",
        email = "you@example.com",
    )

    val workspaces = listOf(
        Workspace(id = "ws_default", name = "Default Workspace", slug = "default", memberCount = 4),
    )

    val runtimes = listOf(
        Runtime(
            id = "rt_1",
            name = "Local Agent",
            type = "local",
            state = "online",
            agents = listOf("claude", "codex", "copilot", "gemini", "opencode"),
            deviceName = "mexs-laptop",
            profile = "default",
            uptime = "4h 12m",
        ),
        Runtime(
            id = "rt_2",
            name = "office-pc",
            type = "local",
            state = "offline",
            agents = listOf("claude", "codex"),
            deviceName = "office-pc",
            uptime = "—",
        ),
    )

    val agents = listOf(
        Agent(
            id = "a_lambda",
            name = "Lambda",
            provider = "claude",
            runtimeId = "rt_1",
            state = "busy",
        ),
        Agent(
            id = "a_frontend",
            name = "Frontend",
            provider = "codex",
            runtimeId = "rt_1",
            state = "busy",
        ),
        Agent(
            id = "a_hermes",
            name = "Hermes",
            provider = "opencode",
            runtimeId = "rt_1",
            state = "idle",
        ),
        Agent(
            id = "a_diag",
            name = "Diagnoser",
            provider = "gemini",
            runtimeId = "rt_2",
            state = "offline",
        ),
    )

    val issues = listOf(
        Issue(id = "i_1", key = "MUL-456", title = "Refactor auth middleware to support OIDC", status = "in_progress", priority = "high", assigneeId = "a_lambda", assigneeName = "Lambda", isAgentAssignee = true, updatedAt = "2026-06-05T01:30:00Z", createdAt = "2026-06-04T22:10:00Z"),
        Issue(id = "i_2", key = "MUL-457", title = "Fix dropdown positioning on small screens", status = "in_progress", priority = "medium", assigneeId = "a_frontend", assigneeName = "Frontend", isAgentAssignee = true, updatedAt = "2026-06-05T01:35:00Z", createdAt = "2026-06-04T20:00:00Z"),
        Issue(id = "i_3", key = "MUL-458", title = "Migrate Postgres 16 → 17 in staging", status = "in_progress", priority = "urgent", assigneeId = "a_lambda", assigneeName = "Lambda", isAgentAssignee = true, updatedAt = "2026-06-04T22:50:00Z", createdAt = "2026-06-04T19:30:00Z"),
        Issue(id = "i_4", key = "MUL-460", title = "Add Lark Bot webhook", status = "in_review", priority = "high", assigneeId = "a_hermes", assigneeName = "Hermes", isAgentAssignee = true, updatedAt = "2026-06-04T23:55:00Z", createdAt = "2026-06-04T11:00:00Z"),
        Issue(id = "i_5", key = "MUL-461", title = "Document daemon env vars", status = "todo", priority = "medium", updatedAt = "2026-06-04T22:00:00Z", createdAt = "2026-06-04T18:00:00Z"),
        Issue(id = "i_6", key = "MUL-462", title = "Spike: Pinia vs Zustand for state", status = "backlog", priority = "low", updatedAt = "2026-06-04T15:00:00Z", createdAt = "2026-06-04T15:00:00Z"),
        Issue(id = "i_7", key = "MUL-440", title = "Wire up new billing webhook", status = "done", priority = "high", assigneeId = "a_frontend", assigneeName = "Frontend", isAgentAssignee = true, updatedAt = "2026-06-04T18:00:00Z", createdAt = "2026-06-03T09:00:00Z"),
        Issue(id = "i_8", key = "MUL-441", title = "Bump deps in /apps/web", status = "done", priority = "medium", assigneeId = "a_lambda", assigneeName = "Lambda", isAgentAssignee = true, updatedAt = "2026-06-04T17:00:00Z", createdAt = "2026-06-03T10:00:00Z"),
        Issue(id = "i_9", key = "MUL-450", title = "Rate limit agent poll loop", status = "blocked", priority = "urgent", assigneeId = "a_lambda", assigneeName = "Lambda", isAgentAssignee = true, updatedAt = "2026-06-04T20:00:00Z", createdAt = "2026-06-04T13:00:00Z"),
        Issue(id = "i_10", key = "MUL-451", title = "Cleanup .agent_context sidecar", status = "cancelled", priority = "low", updatedAt = "2026-06-04T20:00:00Z", createdAt = "2026-06-04T13:00:00Z"),
    )

    val daemonStatus = DaemonStatus(
        state = "running",
        pid = 24148,
        uptime = "4h 12m",
        daemonId = "mexs-laptop",
        deviceName = "mexs-laptop",
        agents = listOf("claude", "codex", "copilot", "gemini", "opencode"),
        workspaceCount = 1,
        profile = "default",
        serverUrl = "http://192.168.1.10:8080",
    )
}
