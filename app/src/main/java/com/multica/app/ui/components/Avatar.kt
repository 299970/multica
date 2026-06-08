package com.multica.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

/**
 * v0.3.15 新增：agent 头像（multica server `avatar_url`）
 *  - 优先加载 server URL（拼上 BASE_URL = http://172.26.28.80:9090）
 *  - 加载失败 / URL 为空 → 显示 fallback（agent name 首字）
 *  - 圆形裁剪，size 24-32dp
 */
@Composable
fun Avatar(
    url: String?,
    size: Dp = 32.dp,
    fallback: String = "?",
) {
    val ctx = LocalContext.current
    if (url.isNullOrBlank()) {
        FallbackAvatar(size, fallback)
    } else {
        val fullUrl = if (url.startsWith("http")) url else "http://172.26.28.80:9090$url"
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(fullUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF3A3A3C)),
            error = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF3A3A3C)),
            fallback = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF3A3A3C)),
            onError = { /* 加载失败也不显示 log（Coil 默认 log） */ },
        )
    }
}

@Composable
private fun FallbackAvatar(size: Dp, text: String) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF3A3A3C)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.take(1).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFF5F5F7),
        )
    }
}
