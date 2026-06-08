package com.multica.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.multica.app.ui.nav.MulticaNav
import com.multica.app.ui.theme.MulticaAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 需求 #1：屏幕保持常亮（专为大屏状态展示）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 需求 #2（2026-06-08 老板新增）：全屏显示，隐藏状态栏
        // 1) 边到边布局：内容延伸到状态栏 / 导航栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 2) 隐藏 status bar（顶部）和 navigation bar（底部）
        // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE：用户从屏幕边缘滑可临时显示（系统手势）
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MulticaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MulticaNav()
                }
            }
        }
    }
}
