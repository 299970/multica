package com.multica.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multica.app.data.model.Agent
import com.multica.app.data.model.DaemonStatus
import com.multica.app.data.model.InboxItem
import com.multica.app.data.model.Issue
import com.multica.app.data.model.IssueComment
import com.multica.app.data.model.IssueDetail
import com.multica.app.data.model.Runtime
import com.multica.app.data.model.User
import com.multica.app.data.model.Workspace
import android.app.Application
import com.multica.app.data.prefs.SettingsRepository
import com.multica.app.data.repo.MulticaRepository
import com.multica.app.data.ws.WsEvent
import com.multica.app.data.ws.WsState
import com.multica.app.ui.mock.MockData
import com.multica.app.util.NotificationSound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isMock: Boolean = false,
    val errorMessage: String? = null,
    val user: User? = null,
    val workspaces: List<Workspace> = emptyList(),
    val activeWorkspaceId: String? = null,
    val runtimes: List<Runtime> = emptyList(),
    val agents: List<Agent> = emptyList(),
    val issues: List<Issue> = emptyList(),
    val daemonStatus: DaemonStatus? = null,
    val wsState: WsState = WsState.Disconnected,
    // === Boss Tab 数据 ===
    val bossInbox: List<InboxItem> = emptyList(),                  // 全部通知
    val bossMyIssues: List<Issue> = emptyList(),                    // @will / assignee = me
    val bossReviewIssues: List<Issue> = emptyList(),                // 需审核（in_review）
    val bossChatMessages: List<InboxItem> = emptyList(),           // chat 消息（type=chat/new_comment）
) {
    /** Boss Tab 计数（显示在 Tab 标题上） */
    // v0.3.19: bossCount 顶 tab 计数 = 只算**未读** inbox（已客户端过滤）+ myIssues + review
    // 之前 bug：count { !it.read } + inbox.size 双重计入（inbox 已未读，但再 count 一遍 → 数字翻倍 100 → 800+）
    val bossCount: Int get() = bossInbox.size + bossMyIssues.size + bossReviewIssues.size

    /** v0.3.13: runtimes tab 数量按 host 数算（一台主机一个卡片） */
    /** v0.3.18: 排除 done/cancelled 的活跃 issue 数（用于顶部 tab 计数） */
    val activeIssuesCount: Int get() = issues.count { it.status !in setOf("done", "cancelled") }

    val runtimesHostCount: Int get() = runtimes
        .groupBy {
            val name = it.name
            val idx = name.lastIndexOf('(')
            if (idx >= 0 && name.endsWith(")")) name.substring(idx + 1, name.length - 1) else it.id.take(6)
        }
        .size

    /** 当前活动 workspace 的 slug（用于 Boss / Issue 详情等 query） */
    fun activeWorkspaceSlug(): String? = workspaces.firstOrNull { it.id == activeWorkspaceId }?.slug
}

/** Issue 详情页局部状态 */
data class IssueDetailState(
    val isLoading: Boolean = false,
    val detail: IssueDetail? = null,
    val comments: List<IssueComment> = emptyList(),
    val error: String? = null,
)

class DashboardViewModel(
    private val app: Application,
    private val repo: MulticaRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    // v0.3.20: 上一次状态快照（用于 diff 状态变化 → 播放声音）
    private data class PrevState(
        val runtimeStates: Map<String, String> = emptyMap(),  // id -> state
        val agentStates: Map<String, String> = emptyMap(),    // id -> state
    )
    private var prev: PrevState = PrevState()

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    // === Issue 详情（v0.3.5 新增） ===
    private val _issueDetail = MutableStateFlow(IssueDetailState())
    val issueDetail: StateFlow<IssueDetailState> = _issueDetail.asStateFlow()
    private val _navigateIssueId = MutableStateFlow<String?>(null)
    val navigateIssueId: StateFlow<String?> = _navigateIssueId.asStateFlow()

    fun openIssue(issueId: String) {
        _navigateIssueId.value = issueId
        loadIssueDetail(issueId)
    }
    fun closeIssue() {
        _navigateIssueId.value = null
        _issueDetail.value = IssueDetailState()
    }
    fun loadIssueDetail(issueId: String) {
        val slug = _state.value.activeWorkspaceSlug() ?: return
        viewModelScope.launch {
            _issueDetail.value = IssueDetailState(isLoading = true)
            val dR = repo.issueDetail(slug, issueId)
            val cR = repo.issueComments(slug, issueId)
            _issueDetail.value = IssueDetailState(
                isLoading = false,
                detail = dR.getOrNull(),
                comments = cR.getOrNull().orEmpty(),
                error = listOfNotNull(dR.exceptionOrNull()?.message, cR.exceptionOrNull()?.message)
                    .firstOrNull(),
            )
        }
    }

    init {
        // 观察 WS 状态
        viewModelScope.launch {
            repo.wsState.collect { s -> _state.update { it.copy(wsState = s) } }
        }
        // === v0.3.10 兜底：每 3s 主动 poll runtimes + agents + issues ===
        // （v0.3.10 老板需求：agent 状态更新不够实时；
        //   缩短到 3s + 同时拉 issues 用来推算 in_progress agent 工作状态）
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(3_000)
                val slug = _state.value.activeWorkspaceSlug() ?: continue
                viewModelScope.launch {
                    val rR = repo.runtimes(slug)
                    val aR = repo.agents(slug)
                    val iR = repo.issues(slug)
                    _state.update {
                        it.copy(
                            runtimes = rR.getOrNull() ?: it.runtimes,
                            agents = aR.getOrNull() ?: it.agents,
                            issues = iR.getOrNull() ?: it.issues,
                        )
                    }
                }
            }
        }
        // 观察 WS 事件 → 触发 reload
        viewModelScope.launch {
            repo.wsEvents.collect { e ->
                if (e.isIssue || e.isAgent || e.isRuntime) {
                    refresh()
                }
            }
        }
        // 启动 WS（如果已配置）
        if (settings.current.isConfigured) {
            repo.startWs()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val s = settings.current
            if (!s.isConfigured) {
                _state.update { it.copy(isLoading = false, isMock = true, errorMessage = "未配置服务器，显示示例数据") }
                applyMock()
                return@launch
            }
            val meR = repo.me()
            val wsR = repo.workspaces()
            val wsList = wsR.getOrNull().orEmpty()
            // 用 slug 优先（多 workspace 友好）；没填 slug 时取第一个
            val activeSlug = s.workspaceId.takeIf { it.isNotBlank() }
                ?: wsList.firstOrNull()?.slug
                ?: wsList.firstOrNull()?.id
            if (activeSlug == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isMock = true,
                        errorMessage = "未找到工作区，显示示例数据",
                        workspaces = wsR.getOrNull() ?: emptyList(),
                    )
                }
                applyMock()
                return@launch
            }
            val activeWs = wsList.firstOrNull { it.slug == activeSlug || it.id == activeSlug }?.id
            val agentsR = repo.agents(activeSlug)
            val issuesR = repo.issues(activeSlug)
            val runtimeR = repo.runtimes(activeSlug)
            val daemonR = repo.daemonStatus(activeSlug)
            // 通知 Repository 当前 active workspace 的 slug（WS 用）
            repo.setActiveWorkspaceSlug(activeSlug)

            _state.update {
                it.copy(
                    isLoading = false,
                    isMock = false,
                    user = meR.getOrNull(),
                    workspaces = wsR.getOrNull() ?: emptyList(),
                    activeWorkspaceId = activeWs,
                    agents = agentsR.getOrNull() ?: emptyList(),
                    issues = issuesR.getOrNull() ?: emptyList(),
                    runtimes = runtimeR.getOrNull() ?: emptyList(),
                    daemonStatus = daemonR.getOrNull(),
                    errorMessage = listOfNotNull(
                        meR.exceptionOrNull()?.message,
                        wsR.exceptionOrNull()?.message,
                        agentsR.exceptionOrNull()?.message,
                        issuesR.exceptionOrNull()?.message,
                    ).firstOrNull(),
                )
            }

            // === Boss Tab 数据 ===
            val myId = meR.getOrNull()?.id
            // v0.3.16: server 端 unreadOnly=true（只拉未读）+ 客户端再过滤（双保险）
            val inboxR = repo.inbox(activeSlug, unreadOnly = true)
            val myIssuesR = if (myId != null) repo.issuesForAssignee(activeSlug, myId) else Result.success(emptyList<Issue>())
            val reviewR = repo.issuesByStatus(activeSlug, "in_review")
            // v0.3.19: server `unread_only` / `limit` 参数**实际**都没生效（curl 验证：传 true 仍返 839 条，含 30 已读）
            //  → 客户端**强制**只保留未读 + 只看最近 7 天（避免历史 spam 把计数顶到 800+）
            val cutoff = java.time.OffsetDateTime.now().minusDays(7)
            val allInbox = inboxR.getOrNull().orEmpty().filter { item ->
                !item.read && (try {
                    val odt = java.time.OffsetDateTime.parse(item.createdAt ?: "")
                    odt.isAfter(cutoff)
                } catch (_: Throwable) { true })
            }
            val allMyIssues = myIssuesR.getOrNull().orEmpty()
            val allReview = reviewR.getOrNull().orEmpty()
            // v0.3.13: bossChatMessages 只显示**未读**消息（老板 2026-06-08 反馈：之前所有消息都显示）
            val allChat = allInbox.filter { it.isChat && !it.read }
            // v0.3.20: 状态变化 diff + 播放声音
            val currentRuntimes = runtimeR.getOrNull().orEmpty()
            val currentAgents = agentsR.getOrNull().orEmpty()
            detectStateChangesAndPlay(currentRuntimes, currentAgents)
            _state.update {
                it.copy(
                    bossInbox = allInbox,
                    bossMyIssues = allMyIssues,
                    bossReviewIssues = allReview,
                    bossChatMessages = allChat,
                )
            }
            // 保证 WS 跑着
            repo.startWs()
        }
    }

    fun setWorkspace(id: String) {
        _state.update { it.copy(activeWorkspaceId = id) }
        refresh()
    }

    /**
     * v0.3.20: diff 上次/本次状态
     *  - runtime 状态从非 online → online  → 叮（上线）
     *  - runtime 状态从 online → 非 online → 叮（离线；用一个声音）
     *  - agent 状态从非 busy → busy       → 叮（任务开始）
     *  - agent 状态从 busy → 非 busy      → 嘟（任务结束）
     */
    private fun detectStateChangesAndPlay(runtimes: List<Runtime>, agents: List<Agent>) {
        val newRuntimeStates = runtimes.associate { it.id to it.state }
        val newAgentStates = agents.associate { it.id to it.state }

        // runtime 状态变化
        for ((id, newSt) in newRuntimeStates) {
            val oldSt = prev.runtimeStates[id]
            if (oldSt != null && oldSt != newSt) {
                NotificationSound.ding(app)  // 上线/离线都叮（老板需求）
            }
        }
        // agent 状态变化
        for ((id, newSt) in newAgentStates) {
            val oldSt = prev.agentStates[id]
            if (oldSt != null && oldSt != newSt) {
                val wasBusy = oldSt in setOf("busy", "in_progress", "working", "running", "active")
                val isBusy = newSt in setOf("busy", "in_progress", "working", "running", "active")
                when {
                    !wasBusy && isBusy -> NotificationSound.ding(app)   // 任务开始 → 叮
                    wasBusy && !isBusy -> NotificationSound.dong(app)   // 任务结束 → 嘟
                }
            }
        }
        prev = PrevState(newRuntimeStates, newAgentStates)
    }

    private fun applyMock() {
        _state.update {
            it.copy(
                user = MockData.user,
                workspaces = MockData.workspaces,
                activeWorkspaceId = MockData.workspaces.first().id,
                runtimes = MockData.runtimes,
                agents = MockData.agents,
                issues = MockData.issues,
                daemonStatus = MockData.daemonStatus,
            )
        }
    }
}
