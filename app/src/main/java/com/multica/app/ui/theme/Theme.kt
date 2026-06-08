package com.multica.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 需求 #2：暗色 + 苹果风（黑底 + 高对比 + SF 风格色板）
// 直接用 darkColorScheme，禁用 dynamicColor 以保持一致的"展示风"
private val MulticaDark = darkColorScheme(
    background = Color(0xFF000000),    // 纯黑（要求）
    surface = Color(0xFF111111),        // 卡片 1 级
    surfaceVariant = Color(0xFF1C1C1E), // 卡片 2 级（iOS-style elevated）
    onBackground = Color(0xFFF5F5F7),  // iOS system gray 6
    onSurface = Color(0xFFF5F5F7),
    onSurfaceVariant = Color(0xFFA1A1A6), // iOS system gray
    primary = Color(0xFF0A84FF),       // iOS system blue
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF5E5CE6),      // iOS system indigo
    tertiary = Color(0xFFFF9F0A),       // iOS system orange (warn)
    error = Color(0xFFFF453A),         // iOS system red
    outline = Color(0xFF2C2C2E),       // iOS separator
    outlineVariant = Color(0xFF38383A),
)

@Composable
fun MulticaAppTheme(
    content: @Composable () -> Unit,
) {
    // 需求 #3：紧凑型 — 通过 MaterialTheme 的 spacing 调小（后续组件统一用 small padding）
    MaterialTheme(
        colorScheme = MulticaDark,
        content = content,
    )
}
