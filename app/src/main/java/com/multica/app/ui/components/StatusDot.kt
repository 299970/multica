package com.multica.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 状态圆点 — 老板 2026-06-08 更新要求 4 档：
 *  - 绿 = 在线
 *  - 红 = 离线
 *  - 黄 = 异常 / 启动中
 *  - 灰 = 未知
 */
@Composable
fun StatusDot(
    state: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(stateDotColor(state))
    )
}

/** 4 档状态色 */
fun stateDotColor(state: String): Color = when (state.lowercase()) {
    // 在线（绿色）
    "online", "running", "connected", "active" -> Color(0xFF22C55E)
    // 异常 / 启动中（黄色）
    "starting", "connecting", "busy", "in_progress", "blocked" -> Color(0xFFEAB308)
    // 离线（红色）
    "offline", "stopped", "disconnected", "failed", "error", "cancelled", "stopping" -> Color(0xFFEF4444)
    // 未知 / 空闲（灰色）— 注意 idle 在 4 档语义里不算"在线"
    "idle", "ready", "todo", "backlog", "done", "completed" -> Color(0xFF9CA3AF)
    else -> Color(0xFF9CA3AF)
}

/** 完整版 status label（保留中文翻译） */
fun stateColor(state: String): Pair<Color, String> = when (state.lowercase()) {
    "online", "running", "connected", "active" -> Color(0xFF22C55E) to "在线"
    "starting", "connecting" -> Color(0xFFEAB308) to "连接中"
    "stopping" -> Color(0xFFF97316) to "停止中"
    "offline", "stopped", "disconnected" -> Color(0xFF9CA3AF) to "离线"
    "failed", "error" -> Color(0xFFEF4444) to "失败"
    "busy", "in_progress" -> Color(0xFF3B82F6) to "忙碌"
    "idle" -> Color(0xFF60A5FA) to "空闲"
    "blocked" -> Color(0xFFF43F5E) to "阻塞"
    "done", "completed" -> Color(0xFF10B981) to "完成"
    "cancelled" -> Color(0xFF6B7280) to "已取消"
    "in_review" -> Color(0xFFA855F7) to "评审中"
    "todo" -> Color(0xFF94A3B8) to "待办"
    "backlog" -> Color(0xFFCBD5E1) to "待规划"
    else -> Color(0xFFCBD5E1) to state
}

fun statusLabel(state: String): String = stateColor(state).second
