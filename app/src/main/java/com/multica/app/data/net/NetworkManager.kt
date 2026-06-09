package com.multica.app.data.net

import android.util.Log
import com.multica.app.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * v0.3.29 NetworkManager（老板 2026-06-08 需求）：
 *  - 启动优先连内网（lanUrl），内网不通才连域名（wanUrl）
 *  - 暴露 NetState：Internal（绿）/ External（蓝）/ Failed（红）
 *  - 60s 重探（内网恢复会自动切回去）
 *  - 内网客户端 2s connect timeout（探测必须快）；外网 10s
 *
 * 重要：v0.3.27 我以为建了这个文件，但实际没建（grep 找不到）。
 *      所以之前"内网优先"是空话，v0.3.29 真正落地。
 */
class NetworkManager(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val tag = "MulticaNet"

    /** 内部客户端：探测用，超时短 */
    private val probeClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    /** 业务客户端：内网也用这个（5s connect，10s read） */
    private val bizClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** 业务客户端：外网用（10s connect，30s read） */
    private val bizClientExternal: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow<NetState>(NetState.Unknown)
    val state: StateFlow<NetState> = _state.asStateFlow()

    /**
     * 当前业务用的 baseUrl（无尾 /）。
     * UI 应当用 state 决定颜色显示；Repository 用 currentBaseUrl 实际发请求。
     */
    val currentBaseUrl: String
        get() {
            val st = _state.value
            return when (st) {
                is NetState.Internal -> st.url
                is NetState.External -> st.url
                is NetState.Failed -> settings.current.lanUrl
                    .ifBlank { settings.current.wanUrl }
                    .ifBlank { settings.current.serverUrl }
                    .trimEnd('/')
                is NetState.Unknown -> settings.current.serverUrl.trimEnd('/')
            }
        }

    /** 业务 HTTP client（OkHttp） */
    val httpClient: OkHttpClient
        get() = when (_state.value) {
            is NetState.External -> bizClientExternal
            else -> bizClient
        }

    /** 启动后台 probe 循环 + 立即探测一次 */
    fun start() {
        scope.launch(Dispatchers.IO) {
            probe()  // 启动立即探
            while (true) {
                delay(60_000)
                probe()  // 60s 重探
            }
        }
    }

    /** 手动重新探测（设置页保存后调用） */
    suspend fun reprobe() = probe()

    private suspend fun probe() {
        val s = settings.current
        val lan = s.lanUrl.trimEnd('/').ifBlank { null }
        val wan = s.wanUrl.trimEnd('/').ifBlank { null }
        val fallback = s.serverUrl.trimEnd('/').ifBlank { null }
        Log.d(tag, "probing: lan=$lan wan=$wan fb=$fallback")

        if (lan != null && probeOne(lan)) {
            Log.d(tag, "✓ 内网 OK, using 内网: $lan")
            _state.value = NetState.Internal(lan)
            return
        }
        if (wan != null && probeOne(wan)) {
            Log.d(tag, "✓ 域名 OK, using 域名: $wan")
            _state.value = NetState.External(wan)
            return
        }
        if (fallback != null && probeOne(fallback)) {
            // fallback 走 lan/wan 都不通时，看 fallback 是不是 lan/wan 之一
            val st = when (fallback) {
                lan -> NetState.Internal(fallback)
                wan -> NetState.External(fallback)
                else -> NetState.External(fallback)
            }
            Log.d(tag, "✓ fallback OK, using $st")
            _state.value = st
            return
        }
        Log.w(tag, "✗ 全部不可达, mark Failed")
        _state.value = NetState.Failed
    }

    private suspend fun probeOne(base: String): Boolean = withTimeoutOrNull(2_500) {
        runCatching {
            val req = Request.Builder()
                .url("$base/api/health")
                .header("Accept", "application/json")
                .build()
            probeClient.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        }.getOrDefault(false)
    } ?: false

    sealed class NetState {
        object Unknown : NetState()
        data class Internal(val url: String) : NetState()
        data class External(val url: String) : NetState()
        object Failed : NetState()
    }
}
