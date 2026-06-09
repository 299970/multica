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

    // v0.3.30: 探测中标记（UI 显示转圈动画）
    private val _probing = MutableStateFlow(false)
    val probing: StateFlow<Boolean> = _probing.asStateFlow()

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
                is NetState.Probing -> settings.current.lanUrl
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

    /**
     * v0.3.30 老板 2026-06-09 新需求：
     *  - 启动优先连内网
     *  - **如果内网 1 分钟还没成功，自动切到域名**
     *  - 启动后每 60 秒重探一次（看是否要切回内网）
     */
    fun start() {
        scope.launch(Dispatchers.IO) {
            // === 阶段 1: 启动试内网 1 分钟 ===
            _probing.value = true
            _state.value = NetState.Probing  // UI 显示转圈
            Log.d(tag, "启动：先试内网（最长 60s）")
            val lan = settings.current.lanUrl.trimEnd('/').ifBlank { null }
            val wan = settings.current.wanUrl.trimEnd('/').ifBlank { null }

            if (lan != null) {
                // 试 1 分钟：每 2 秒一次（30 次机会）
                val deadline = System.currentTimeMillis() + 60_000
                var gotLan = false
                while (System.currentTimeMillis() < deadline) {
                    if (probeOne(lan)) {
                        Log.d(tag, "✓ 1 分钟内内网 OK, using 内网: $lan")
                        _state.value = NetState.Internal(lan)
                        _probing.value = false
                        gotLan = true
                        break
                    }
                    delay(2_000)
                }
                if (!gotLan) {
                    Log.w(tag, "✗ 1 分钟内内网没通，fallback 到域名")
                }
            }

            // === 阶段 2: 内网 1 分钟没通，强制切域名 ===
            if (_state.value !is NetState.Internal && wan != null) {
                if (probeOne(wan)) {
                    Log.d(tag, "✓ 域名 OK, using 域名: $wan")
                    _state.value = NetState.External(wan)
                } else {
                    Log.w(tag, "✗ 域名也不通，mark Failed")
                    _state.value = NetState.Failed
                }
            } else if (_state.value !is NetState.Internal) {
                _state.value = NetState.Failed
            }
            _probing.value = false

            // === 阶段 3: 60s 周期重探（看内网是否恢复，恢复了切回） ===
            while (true) {
                delay(60_000)
                _probing.value = true
                probe()  // 用原本的 probe 逻辑（lan → wan → fallback）
                _probing.value = false
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

    /**
     * v0.3.32 老板 2026-06-09 需求："内网=绿，域名=蓝，无法连接=红"
     *  - 之前用 /api/me，server 返回 401/404/500 → 标红
     *  - 现在改成**最宽松判断**：只要 server 回了**任何 HTTP 响应**（不是 connect refused / timeout）= 绿色
     *  - 5xx 也算通（业务层错算 server 活着）
     *  - 真正"无法连接"只有 connect refused / timeout（IOException / SocketTimeoutException）
     */
    private suspend fun probeOne(base: String): Boolean = withTimeoutOrNull(3_500) {
        runCatching {
            val pat = settings.current.pat
            val req = Request.Builder()
                .url("$base/api/me")
                .header("Accept", "application/json")
                .apply { if (pat.isNotBlank()) header("Authorization", "Bearer $pat") }
                .build()
            probeClient.newCall(req).execute().use { resp ->
                Log.d(tag, "probe $base → HTTP ${resp.code} (reachable, treating as UP)")
                // 任何 HTTP 响应 = server 在监听 9090 = "endpoint 可达"
                // connect refused / read timeout 才返回 false（runCatching catch 不到 IOException）
                true
            }
        }.getOrElse { e ->
            Log.d(tag, "probe $base → ${e.javaClass.simpleName}: ${e.message} (network error, DOWN)")
            false
        }
    } ?: run {
        Log.d(tag, "probe $base → timeout (DOWN)")
        false
    }

    sealed class NetState {
        object Unknown : NetState()
        /** v0.3.30: 正在探测（启动 1 分钟内） */
        object Probing : NetState()
        data class Internal(val url: String) : NetState()
        data class External(val url: String) : NetState()
        object Failed : NetState()
    }
}
