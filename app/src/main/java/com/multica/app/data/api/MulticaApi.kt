package com.multica.app.data.api

import com.multica.app.data.model.Agent
import com.multica.app.data.model.AgentTask
import com.multica.app.data.model.DailyUsage
import com.multica.app.data.model.DailyUsageResponse
import com.multica.app.data.model.DaemonStatus
import com.multica.app.data.model.GitHubRelease
import com.multica.app.data.model.InboxItem
import com.multica.app.data.model.Issue
import com.multica.app.data.model.IssueListResponse
import com.multica.app.data.model.PagedResponse
import com.multica.app.data.model.Runtime
import com.multica.app.data.model.User
import com.multica.app.data.model.Workspace
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Multica Server REST API
 *
 * 路径约定（从 multica 仓库 server/cmd/server/router.go 提取）：
 *  - GET  /api/me                          → User
 *  - GET  /api/workspaces                  → Workspace[]
 *  - GET  /api/workspaces/{id}/agents      → Agent[]
 *  - GET  /api/workspaces/{id}/issues      → Issue[]
 *  - GET  /api/workspaces/{id}/runtimes    → Runtime[]
 *  - GET  /api/daemon                      → Daemon status（per-workspace）
 *  - WS   /ws?token=<PAT>                  → 实时事件流
 *
 * 字段命名兼容 snake_case（Retrofit + kotlinx-serialization 默认）。
 */
interface MulticaApi {

    @GET("api/me")
    suspend fun me(): User

    /**
     * 服务端真实返回：
     *   - `/api/agents` → `[...]` 裸数组
     *   - `/api/runtimes` → `[...]` 裸数组
     *   - `/api/issues` → `{"issues":[...]}` 包了一层
     *   - `/api/workspaces` → `[...]` 裸数组
     * 都是**顶层**路由（不是 nested 在 workspaces 下面），且必须带 workspace_slug query。
     */
    @GET("api/workspaces")
    suspend fun workspaces(): List<Workspace>

    @GET("api/agents")
    suspend fun agents(
        @Query("workspace_slug") workspaceSlug: String,
        @Query("limit") limit: Int = 100,
    ): List<Agent>

    @GET("api/issues")
    suspend fun issues(
        @Query("workspace_slug") workspaceSlug: String,
        @Query("status") status: String? = null,
        @Query("assignee_id") assigneeId: String? = null,
        @Query("limit") limit: Int = 100,
    ): IssueListResponse

    @GET("api/runtimes")
    suspend fun runtimes(
        @Query("workspace_slug") workspaceSlug: String,
        @Query("limit") limit: Int = 50,
    ): List<Runtime>

    /**
     * Per-workspace daemon status 顶层。
     * 注：daemon 当前只能通过 WS 推送或 runtime 列表推算（runtime.online 字段）。
     * 这个接口主要给前端 dashboard 用，response 结构：{workspace_id, daemons:[...]}
     */
    @GET("api/daemon")
    suspend fun daemonStatus(
        @Query("workspace_slug") workspaceSlug: String? = null,
    ): DaemonStatus

    /**
     * Boss Tab 用 — server `/api/inbox` 返回**裸数组**
     *  包含 type=new_comment / mention / task_assigned / chat 等
     */
    @GET("api/inbox")
    suspend fun inbox(
        @Query("workspace_slug") workspaceSlug: String,
        @Query("unread_only") unreadOnly: Boolean = false,
        @Query("limit") limit: Int = 100,
    ): List<InboxItem>

    /**
     * Boss Tab 用 — assignee 给我自己的 issues
     *  server `/api/issues?assignee_id=xxx&workspace_slug=xxx` 返回 {"issues":[...]}
     */
    @GET("api/issues")
    suspend fun issuesForAssignee(
        @Query("workspace_slug") workspaceSlug: String,
        @Query("assignee_id") assigneeId: String,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
    ): IssueListResponse

    /**
     * Boss Tab 用 — 需要审核的 issues（in_review 状态）
     *  server `/api/issues?status=in_review` → `{"issues":[...]}`
     */
    @GET("api/issues")
    suspend fun issuesByStatus(
        @Query("workspace_slug") workspaceSlug: String,
        @Query("status") status: String,
        @Query("limit") limit: Int = 50,
    ): IssueListResponse

    /**
     * Issue 详情 — `GET /api/issues/{issueId}?workspace_slug=xxx` → IssueDetail
     *  含 description / attachments / labels
     */
    @GET("api/issues/{issueId}")
    suspend fun issueDetail(
        @Path("issueId") issueId: String,
        @Query("workspace_slug") workspaceSlug: String,
    ): com.multica.app.data.model.IssueDetail

    /**
     * v0.3.34 老板 2026-06-09 新增 — 每日 token 用量（柱状图数据源）
     * server 端先试 `/api/dashboard/usage?workspace_slug=xxx&days=30`
     * 失败 fallback `/api/usage/daily?workspace_slug=xxx&days=30`
     * 再失败 fallback `/api/tokens/daily` (POST)
     * 字段兼容 `days` / `data` / `items` / `usage` 任一顶层数组
     */
    @GET("api/dashboard/usage")
    suspend fun dailyUsage(
        @Query("workspace_slug") workspaceSlug: String,
        @Query("workspace_id") workspaceId: String? = null,
        @Query("days") days: Int = 30,
    ): DailyUsageResponse

    @GET("api/usage/daily")
    suspend fun dailyUsageAlt(
        @Query("workspace_slug") workspaceSlug: String,
        @Query("days") days: Int = 30,
    ): DailyUsageResponse

    /**
     * Issue 评论 — `GET /api/issues/{issueId}/comments` → Comment[]（裸数组）
     */
    @GET("api/issues/{issueId}/comments")
    suspend fun issueComments(
        @Path("issueId") issueId: String,
        @Query("workspace_slug") workspaceSlug: String,
    ): List<com.multica.app.data.model.IssueComment>

    /** Agent 任务列表 — `GET /api/agents/{agentId}/tasks` → AgentTask[]（裸数组） */
    @GET("api/agents/{agentId}/tasks")
    suspend fun agentTasks(
        @Path("agentId") agentId: String,
        @Query("workspace_slug") workspaceSlug: String,
    ): List<AgentTask>
}

/**
 * GitHub API — 独立 Retrofit 实例（baseUrl = https://api.github.com/）
 * 用于获取 multica 最新 release 版本
 */
interface GitHubApi {
    @GET("repos/multica-ai/multica/releases/latest")
    suspend fun latestRelease(): GitHubRelease
}
