package com.multica.app.di

import android.content.Context
import com.multica.app.data.prefs.SettingsRepository
import com.multica.app.data.repo.MulticaRepository

/**
 * 极简服务定位器（不引入 Hilt，省 KSP / kapt 的麻烦）。
 * 进程内单例。Application onCreate 时初始化一次。
 */
object ServiceLocator {
    private lateinit var _appContext: Context
    private lateinit var _settings: SettingsRepository
    private lateinit var _repo: MulticaRepository

    fun init(ctx: Context) {
        _appContext = ctx.applicationContext
        _settings = SettingsRepository(_appContext)
        val s = _settings.current
        _repo = MulticaRepository(s.serverUrl, s.pat)
    }

    val settings: SettingsRepository get() = _settings
    val repo: MulticaRepository get() = _repo
    val appContext: Context get() = _appContext
}
