package com.multica.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.multica.app.data.ws.WsState

@Composable
fun ConnectionBanner(
    serverUrl: String,
    wsState: WsState,
    isMock: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val (color, label) = when {
        isMock -> Color(0xFFF59E0B) to "示例数据"
        wsState == WsState.Connected -> Color(0xFF22C55E) to "WS 已连接"
        wsState == WsState.Connecting -> Color(0xFFEAB308) to "WS 连接中"
        wsState == WsState.Failed -> Color(0xFFEF4444) to "WS 失败"
        else -> Color(0xFF9CA3AF) to "WS 离线"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusDot(state = when (wsState) {
                WsState.Connected -> "online"
                WsState.Connecting -> "starting"
                WsState.Failed -> "failed"
                WsState.Disconnected -> "offline"
            })
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (serverUrl.isNotBlank()) {
                Text(
                    text = serverUrl,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            errorMessage?.let {
                Text(
                    text = "· ${it.take(80)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
