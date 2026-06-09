package com.multica.app.di

import android.content.Context
import com.multica.app.data.net.NetworkManager
import com.multica.app.data.prefs.SettingsRepository
import com.multica.app.data.repo.MulticaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 极简服务定位器（不引入 Hilt，省 KSP / kapt 的麻烦）。
 * 进程内单例。Application onCreate 时初始化一次。
 */
object ServiceLocator {
    private lateinit var _appContext: Context
    private lateinit var _settings: SettingsRepository
    private lateinit var _repo: MulticaRepository
    private lateinit var _net: NetworkManager
    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(ctx: Context) {
        _appContext = ctx.applicationContext
        _settings = SettingsRepository(_appContext)
        val s = _settings.current
        _repo = MulticaRepository(s.lanUrl.ifBlank { s.wanUrl }.ifBlank { s.serverUrl }, s.pat)
        // v0.3.29: 建 NetworkManager
        _net = NetworkManager(_settings, _scope)
        // v0.3.29: 启动内网/域名探测
        _net.start()
    }

    val settings: SettingsRepository get() = _settings
    val repo: MulticaRepository get() = _repo
    val net: NetworkManager get() = _net
    val appContext: Context get() = _appContext
}
