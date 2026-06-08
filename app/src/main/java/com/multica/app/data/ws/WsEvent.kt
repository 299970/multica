package com.multica.app.data.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * WebSocket 推送到客户端的事件。
 *
 * 服务器发的 JSON 形如：
 * {
 *   "type": "issue.updated",
 *   "payload": { ... }
 * }
 */
@Serializable
data class WsEvent(
    val type: String = "",
    @SerialName("payload")
    val payload: JsonObject? = null,
) {
    val isIssue: Boolean get() = type.startsWith("issue:") || type.startsWith("issue.")
    val isAgent: Boolean get() = type.startsWith("agent:") || type.startsWith("agent.")
    val isRuntime: Boolean get() = type.startsWith("runtime:") || type.startsWith("runtime.") || type.startsWith("daemon:") || type.startsWith("daemon.")
    val isComment: Boolean get() = type.startsWith("comment:") || type.startsWith("comment.")
}

enum class WsState { Disconnected, Connecting, Connected, Failed }
