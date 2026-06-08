package com.multica.app.data.ws

import android.util.Log
import com.multica.app.data.api.MulticaApiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * 包装 OkHttp WebSocket：
 *  - 自动重连（指数退避，最大 30s）
 *  - 首条消息可携带 token（PAT 走 query param；WS 头里没法用 Authorization）
 *  - 暴露 StateFlow<WsState> + SharedFlow<WsEvent>
 */
class MulticaWebSocket(
    private val baseUrl: String,
    private val token: String?,
    private val workspaceSlug: String? = null,
    private val clientVersion: String = "0.2.5",
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client: OkHttpClient = MulticaApiFactory.defaultOkHttp()

    private val _state = MutableStateFlow(WsState.Disconnected)
    val state: StateFlow<WsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private var socket: WebSocket? = null
    private var stopped = false
    private var attempt = 0

    fun start() {
        stopped = false
        scope.launch { loop() }
    }

    fun stop() {
        stopped = true
        socket?.close(1000, "client stop")
        socket = null
        _state.value = WsState.Disconnected
    }

    private suspend fun loop() {
        while (!stopped) {
            val job = scope.launch { connectOnce() }
            // 应用层 ping：每 5s 发一次 text frame `{"type":"ping"}`
            // 原因：multica server 端 WS handler 大约 10s 读超时，10s 内收不到任何 message 就 close。
            // OkHttp 的 `pingInterval` 是 WS 协议层 ping/pong，但 server 端没实现 pong handler，
            // 所以 5s 后 ping 没回 pong → OkHttp 主动 close。
            // 改用应用层 message（text frame），server 读到 message 就会重置读超时。
            val pingJob = scope.launch {
                while (!stopped && job.isActive) {
                    delay(5_000)
                    val s = socket
                    if (s != null) {
                        val ok = s.send("""{"type":"ping"}""")
                        Log.d(TAG, "WS app-ping sent: $ok")
                    }
                }
            }
            job.join()
            pingJob.cancel()
            if (stopped) return
            attempt += 1
            val backoff = (1000L * (1 shl minOf(attempt, 5))).coerceAtMost(30_000L)
            Log.w(TAG, "WS reconnect in ${backoff}ms (attempt=$attempt)")
            delay(backoff)
        }
    }

    private suspend fun connectOnce() {
        // 真实路径（从 multica server/cmd/server/router.go + internal/realtime/hub.go 确认）：
        //   ws://host/ws?workspace_slug=xxx
        // Auth 流程（看 server `firstMessageAuth`）：
        //   1. 升级 WS 后，server 在 10s 内**必须**收到第一条消息
        //   2. 第一条消息必须是 `{"type":"auth","payload":{"token":"<PAT>"}}`
        //   3. Server 回 `{"type":"auth_ack"}` → 连接建立
        //   4. 之后可发 `{"type":"ping"}`（server 回 `{"type":"pong"}`）
        //   5. 可发 `{"type":"subscribe","payload":{"scope":"workspace","id":"<ws_id>"}}` 订阅事件
        val wsBase = baseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
            .trimEnd('/')
        val slug = workspaceSlug ?: "default"
        val url = "$wsBase/ws?workspace_slug=$slug"
        Log.i(TAG, "WS connecting: $url")
        _state.value = WsState.Connecting
        val req = Request.Builder().url(url).build()
        val finished = kotlinx.coroutines.CompletableDeferred<Throwable?>()
        socket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(TAG, "WS open: ${response.code}")
                // onOpen 后**立刻**发 auth 帧（第一帧）
                if (!token.isNullOrBlank()) {
                    val ok = ws.send("""{"type":"auth","payload":{"token":"$token"}}""")
                    Log.i(TAG, "WS auth frame sent: $ok")
                } else {
                    Log.w(TAG, "WS open without token — server will timeout-close in 10s")
                }
            }
            override fun onMessage(ws: WebSocket, text: String) {
                // 收到 auth_ack 才视为认证通过
                if (text.contains("\"type\":\"auth_ack\"")) {
                    Log.i(TAG, "WS auth_ack received → Connected")
                    _state.value = WsState.Connected
                    attempt = 0
                }
                // pong 帧（仅 log 一下）
                if (text.contains("\"type\":\"pong\"")) {
                    Log.d(TAG, "WS pong")
                }
                runCatching { json.decodeFromString(WsEvent.serializer(), text) }
                    .onSuccess { scope.launch { _events.emit(it) } }
                    .onFailure { Log.d(TAG, "WS msg (not WsEvent): $text") }
            }
            override fun onMessage(ws: WebSocket, bytes: ByteString) { /* ignore */ }
            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WS closing: $code $reason")
                ws.close(1000, null)
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WS closed: $code $reason")
                _state.value = WsState.Disconnected
                finished.complete(null)
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WS failure: ${t.message}")
                _state.value = WsState.Failed
                finished.complete(t)
            }
        })
        finished.await()
    }

    companion object {
        private const val TAG = "MulticaWS"
    }
}
