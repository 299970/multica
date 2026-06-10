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
    // === v0.3.29 NetworkManager 网络状态 ===
    val netState: com.multica.app.data.net.NetworkManager.NetState = com.multica.app.data.net.NetworkManager.NetState.Unknown,
    // === v0.3.30 NetworkManager 探测中（UI 转圈） ===
    val netProbing: Boolean = false,
    // === Boss Tab 数据 ===
    val bossInbox: List<InboxItem> = emptyList(),                  // 全部通知
    val bossMyIssues: List<Issue> = emptyList(),                    // @will / assignee = me
    val bossReviewIssues: List<Issue> = emptyList(),                // 需审核（in_review）
    val bossChatMessages: List<InboxItem> = emptyList(),           // chat 消息（type=chat/new_comment）
) {
    /** Boss Tab 计数（显示在 Tab 标题上） */
    // v0.3.21: bossCount = 实际显示的 4 个 section 总数
    //   - @will: bossMyIssues
    //   - 需要审核: bossReviewIssues
    //   - Chat 消息: bossChatMessages
    //   - Inbox 通知: bossInbox 中非 chat 且无 issueId 的（与 BossTab 渲染逻辑一致）
    // 修 v0.3.19/20 的不一致：之前 bossCount = bossInbox.size + myIssues + review = 197
    // 但 inbox 里有 issueId != null 的项没在 Boss Tab 显示（issue 有自己的详情页）→ 数字虚高
    val bossCount: Int get() =
        bossMyIssues.size +
        bossReviewIssues.size +
        bossChatMessages.size +
        bossInbox.count { !it.isChat && it.issueId == null }

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
    private val net: com.multica.app.data.net.NetworkManager,
) : ViewModel() {

    // v0.3.20: 上一次状态快照（用于 diff 状态变化 → 播放声音）
    private data class PrevState(
        val runtimeStates: Map<String, String> = emptyMap(),  // id -> state
        val agentStates: Map<String, String> = emptyMap(),    // id -> state
        val issueStates: Map<String, String> = emptyMap(),    // id -> status (v0.3.21 任务开始/结束声音)
    )
    private var prev: PrevState = PrevState()

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    // v0.3.41: 暴露 agents 列数设置给 UI
    val agentsColsPortrait: Int get() = settings.current.agentsColsPortrait
    val agentsColsLandscape: Int get() = settings.current.agentsColsLandscape

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
        // === v0.3.29 观察 NetworkManager 网络状态 + endpoint 切换 ===
        viewModelScope.launch {
            net.state.collect { st ->
                _state.update { it.copy(netState = st) }
                // endpoint 切换时 rebuild repo（用当前 base url）
                val url = when (st) {
                    is com.multica.app.data.net.NetworkManager.NetState.Internal -> st.url
                    is com.multica.app.data.net.NetworkManager.NetState.External -> st.url
                    else -> null
                }
                if (url != null) {
                    repo.onEndpointChanged(url)
                }
            }
        }
        // === v0.3.30 观察 probing 状态（UI 转圈） ===
        viewModelScope.launch {
            net.probing.collect { p ->
                _state.update { it.copy(netProbing = p) }
            }
        }
        // === v0.3.10/25 兜底：每 3s 主动 poll runtimes + agents + issues + 触发声音检测 ===
        // （v0.3.10 老板需求：agent 状态更新不够实时；
        //   缩短到 3s + 同时拉 issues 用来推算 in_progress agent 工作状态；
        //   v0.3.25 修：之前 polling 走的是 _state.update 路径，**没有调** detectStateChangesAndPlay，
        //     所以状态变化永不触发声音 — 现在 polling 也调 detect）
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(3_000)
                val slug = _state.value.activeWorkspaceSlug() ?: continue
                val rR = repo.runtimes(slug)
                val aR = repo.agents(slug)
                val iR = repo.issues(slug)
                val r = rR.getOrNull() ?: _state.value.runtimes
                val a = aR.getOrNull() ?: _state.value.agents
                val i = iR.getOrNull() ?: _state.value.issues
                _state.update {
                    it.copy(runtimes = r, agents = a, issues = i)
                }
                // v0.3.25: polling 也调声音检测
                detectStateChangesAndPlay(r, a, i)
            }
        }
        // 观察 WS 事件 → 触发 reload
        viewModelScope.launch {
            repo.wsEvents.collect { e ->
                android.util.Log.d("MulticaSound", "WS event: $e")
                if (e.isIssue || e.isAgent || e.isRuntime) {
                    refresh()
                }
            }
        }
        // 启动 WS（如果已配置）
        if (settings.current.isConfigured) {
            repo.startWs()
        }
        // v0.3.23: 启动时**强制**播一次"启动音" — 让老板能立刻确认声音系统工作
        android.util.Log.d("MulticaSound", "init: playing startup ding")
        NotificationSound.ding(app)
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
            // v0.3.33: 如果真实 API 调成功，强制标绿（不依赖 NetworkManager probe）
            // 解决"内网能连也正常使用但显示红色"问题
            meR.onSuccess {
                val url = repo.serverUrl
                _state.update { it.copy(netState = com.multica.app.data.net.NetworkManager.NetState.Internal(url)) }
                android.util.Log.d("MulticaNet", "refresh: me() 成功 → 强制标绿 url=$url")
            }
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
            val currentIssues = issuesR.getOrNull() ?: emptyList()
            detectStateChangesAndPlay(currentRuntimes, currentAgents, currentIssues)
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
     * v0.3.20/21: diff 上次/本次状态
     *  - runtime 状态变化（任何变化）→ 叮（上线/离线/重启）
     *  - agent 状态从非 busy → busy       → 叮（agent 工作；备用）
     *  - agent 状态从 busy → 非 busy      → 嘟
     *  - issue 状态从非 in_progress → in_progress → 叮（任务开始 — 老板 2026-06-08 反馈）
     *  - issue 状态从 in_progress → done/cancelled → 嘟（任务结束）
     *
     *  v0.3.21 修：之前 server agent.state 永远 "idle"，agent 状态变化永不触发
     *  改为 **issue 状态变化**触发声音（issue 状态才会真正变化）
     */
    private fun detectStateChangesAndPlay(runtimes: List<Runtime>, agents: List<Agent>, issues: List<Issue> = emptyList()) {
        val newRuntimeStates = runtimes.associate { it.id to it.state }
        val newAgentStates = agents.associate { it.id to it.state }
        val newIssueStates = issues.associate { it.id to it.status }

        android.util.Log.d("MulticaSound", "detect: runtimes=${newRuntimeStates.size} agents=${newAgentStates.size} issues=${newIssueStates.size}")

        // runtime 状态变化（任何变化都叮）
        for ((id, newSt) in newRuntimeStates) {
            val oldSt = prev.runtimeStates[id]
            if (oldSt != null && oldSt != newSt) {
                android.util.Log.d("MulticaSound", "RUNTIME CHANGED $id: $oldSt -> $newSt → ding")
                NotificationSound.ding(app)
            }
        }
        // agent 状态变化（备用；server 现在不真返回 busy 所以基本不触发）
        for ((id, newSt) in newAgentStates) {
            val oldSt = prev.agentStates[id]
            if (oldSt != null && oldSt != newSt) {
                val wasBusy = oldSt in setOf("busy", "in_progress", "working", "running", "active")
                val isBusy = newSt in setOf("busy", "in_progress", "working", "running", "active")
                android.util.Log.d("MulticaSound", "AGENT CHANGED $id: $oldSt -> $newSt (wasBusy=$wasBusy isBusy=$isBusy)")
                when {
                    !wasBusy && isBusy -> NotificationSound.ding(app)
                    wasBusy && !isBusy -> NotificationSound.dong(app)
                }
            }
        }
        // v0.3.21: issue 状态变化（任务开始/结束的主信号源）
        for ((id, newSt) in newIssueStates) {
            val oldSt = prev.issueStates[id]
            if (oldSt != null && oldSt != newSt) {
                val wasInProgress = oldSt == "in_progress"
                val isInProgress = newSt == "in_progress"
                val wasDone = oldSt in setOf("done", "cancelled")
                val isDone = newSt in setOf("done", "cancelled")
                android.util.Log.d("MulticaSound", "ISSUE CHANGED $id: $oldSt -> $newSt (inProg=$isInProgress done=$isDone)")
                when {
                    // 进入 in_progress = 任务开始 → 叮
                    !wasInProgress && isInProgress -> {
                        android.util.Log.d("MulticaSound", "  → DING (task started)")
                        NotificationSound.ding(app)
                    }
                    // 从 in_progress 退到 done/cancelled = 任务结束 → 嘟
                    wasInProgress && isDone -> {
                        android.util.Log.d("MulticaSound", "  → DONG (task done)")
                        NotificationSound.dong(app)
                    }
                }
            }
        }
        prev = PrevState(newRuntimeStates, newAgentStates, newIssueStates)
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
