package com.multica.app.data.repo

import com.multica.app.data.api.AuthInterceptor
import com.multica.app.data.api.MulticaApi
import com.multica.app.data.api.MulticaApiFactory
import com.multica.app.data.model.Agent
import com.multica.app.data.model.DaemonStatus
import com.multica.app.data.model.Issue
import com.multica.app.data.model.Runtime
import com.multica.app.data.model.User
import com.multica.app.data.model.Workspace
import com.multica.app.data.ws.MulticaWebSocket
import com.multica.app.data.ws.WsEvent
import com.multica.app.data.ws.WsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 仓库层：对 ViewModel 暴露统一入口。
 *
 * - 配置（serverUrl / token）变更时调用 [rebuild] 重建 api + ws
 * - 任何网络异常 → 返回 safe default，让 UI 不白屏
 */
class MulticaRepository(
    serverUrl: String,
    pat: String,
) {
    @Volatile var serverUrl: String = serverUrl
        private set
    @Volatile var pat: String = pat
        private set

    @Volatile private var api: MulticaApi =
        // 防止空 baseUrl 让 Retrofit 抛 IllegalArgumentException 把 APP 启动期就崩
        MulticaApiFactory.build(serverUrl.ifBlank { "http://localhost.invalid/" }, pat)
    @Volatile private var ws: MulticaWebSocket? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _wsState = MutableStateFlow(WsState.Disconnected)
    val wsState: StateFlow<WsState> = _wsState.asStateFlow()

    /** 转发给当前 ws。ws 不存在时返回 emptyFlow，不会 NPE。 */
    val wsEvents: Flow<WsEvent>
        get() = ws?.events ?: emptyFlow()

    fun rebuild(newServerUrl: String, newPat: String) {
        stopWs()
        serverUrl = newServerUrl
        pat = newPat
        api = MulticaApiFactory.build(newServerUrl, newPat)
    }

    fun startWs() {
        if (serverUrl.isBlank() || pat.isBlank()) return
        stopWs()
        val slug = _currentSlug
        val w = MulticaWebSocket(serverUrl, pat, workspaceSlug = slug)
        ws = w
        // 转发状态
        scope.launch { w.state.collect { _wsState.value = it } }
        w.start()
    }

    // === v0.3.29 兼容 NetworkManager 切换 endpoint ===
    /** v0.3.29: NetworkManager 探测到 endpoint 切换时调用，rebuild api + 重启 WS */
    fun onEndpointChanged(newServerUrl: String) {
        if (newServerUrl == serverUrl) return
        android.util.Log.d("MulticaNet", "endpoint changed: $serverUrl -> $newServerUrl, rebuild")
        rebuild(newServerUrl, pat)
    }

    /** 当前激活 workspace 的 slug（给 WS query param 用），由 ViewModel 切 workspace 时更新。 */
    @Volatile private var _currentSlug: String? = null
    fun setActiveWorkspaceSlug(slug: String?) {
        if (_currentSlug == slug) return
        _currentSlug = slug
        // slug 变了重启 WS（不同 workspace 订阅 channel 不同）
        if (ws != null) startWs()
    }

    fun stopWs() {
        ws?.stop()
        ws = null
        _wsState.value = WsState.Disconnected
    }

    suspend fun me(): Result<User> = safe { api.me() }
    suspend fun workspaces(): Result<List<Workspace>> = safe { api.workspaces() }
    suspend fun agents(workspaceSlug: String): Result<List<Agent>> = safe { api.agents(workspaceSlug) }
    suspend fun issues(workspaceSlug: String, status: String? = null): Result<List<Issue>> =
        safe { api.issues(workspaceSlug, status).issues }
    suspend fun runtimes(workspaceSlug: String): Result<List<Runtime>> = safe { api.runtimes(workspaceSlug) }
    suspend fun daemonStatus(workspaceSlug: String?): Result<DaemonStatus> = safe { api.daemonStatus(workspaceSlug) }

    // === Boss Tab API ===
    suspend fun inbox(workspaceSlug: String, unreadOnly: Boolean = false): Result<List<com.multica.app.data.model.InboxItem>> =
        safe { api.inbox(workspaceSlug, unreadOnly) }
    suspend fun issuesForAssignee(workspaceSlug: String, assigneeId: String, status: String? = null): Result<List<Issue>> =
        safe { api.issuesForAssignee(workspaceSlug, assigneeId, status).issues }
    suspend fun issuesByStatus(workspaceSlug: String, status: String): Result<List<Issue>> =
        safe { api.issuesByStatus(workspaceSlug, status).issues }

    // === Issue 详情 API ===
    suspend fun issueDetail(workspaceSlug: String, issueId: String): Result<com.multica.app.data.model.IssueDetail> =
        safe { api.issueDetail(issueId, workspaceSlug) }
    suspend fun issueComments(workspaceSlug: String, issueId: String): Result<List<com.multica.app.data.model.IssueComment>> =
        safe { api.issueComments(issueId, workspaceSlug) }

    private inline fun <T> safe(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Throwable) {
        android.util.Log.e("MulticaRepo", "API call failed: ${e.javaClass.simpleName}: ${e.message}", e)
        Result.failure(e)
    }
}
