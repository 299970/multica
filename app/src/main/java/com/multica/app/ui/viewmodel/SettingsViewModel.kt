package com.multica.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multica.app.data.prefs.SettingsRepository
import com.multica.app.data.repo.MulticaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val lanUrl: String = "",
    val wanUrl: String = "",
    val workspaceId: String = "",
    val pat: String = "",
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val testResult: String? = null,
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val repo: MulticaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        val cur = settings.current
        _state.value = SettingsUiState(
            serverUrl = cur.serverUrl,
            lanUrl = cur.lanUrl,
            wanUrl = cur.wanUrl,
            workspaceId = cur.workspaceId,
            pat = cur.pat,
        )
    }

    fun setServerUrl(v: String) = _state.update { it.copy(serverUrl = v.trim(), errorMessage = null) }
    // v0.3.29: 内网/域名独立地址
    fun setLanUrl(v: String) = _state.update { it.copy(lanUrl = v.trim(), errorMessage = null) }
    fun setWanUrl(v: String) = _state.update { it.copy(wanUrl = v.trim(), errorMessage = null) }
    fun setWorkspaceId(v: String) = _state.update { it.copy(workspaceId = v.trim(), errorMessage = null) }
    fun setPat(v: String) = _state.update { it.copy(pat = v.trim(), errorMessage = null) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            if ((s.lanUrl.isBlank() && s.wanUrl.isBlank() && s.serverUrl.isBlank()) || s.pat.isBlank()) {
                _state.update { it.copy(errorMessage = "至少填一个服务器地址（内网 / 域名）+ PAT") }
                return@launch
            }
            _state.update { it.copy(isWorking = true, errorMessage = null, testResult = null) }
            // 1. 写设置
            settings.update {
                it.copy(
                    serverUrl = s.serverUrl.ifBlank { s.lanUrl },
                    lanUrl = s.lanUrl,
                    wanUrl = s.wanUrl,
                    workspaceId = s.workspaceId,
                    pat = s.pat,
                )
            }
            // 2. 重建 repo（baseUrl 传 lan 优先，repo 内部会按 NetManager 实际切换）
            val base = s.lanUrl.ifBlank { s.wanUrl }.ifBlank { s.serverUrl }
            repo.rebuild(base, s.pat)
            // 3. 测试连接
            val meR = repo.me()
            meR.fold(
                onSuccess = { user ->
                    _state.update {
                        it.copy(
                            isWorking = false,
                            testResult = "连接成功！当前用户: ${user.name.ifBlank { user.email ?: user.id }}",
                        )
                    }
                    // 4. 自动尝试拉工作区
                    val wsR = repo.workspaces()
                    val firstWs = wsR.getOrNull()?.firstOrNull()
                    if (firstWs != null && s.workspaceId.isBlank()) {
                        settings.update { it.copy(workspaceId = firstWs.id) }
                        _state.update { it.copy(workspaceId = firstWs.id) }
                    }
                    // 5. 启动 WS
                    repo.startWs()
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isWorking = false,
                            errorMessage = "连接失败: ${e.message ?: e::class.simpleName}",
                        )
                    }
                }
            )
        }
    }
}
