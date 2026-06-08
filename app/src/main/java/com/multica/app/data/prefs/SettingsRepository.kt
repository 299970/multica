package com.multica.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.multica.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户设置（Server URL、PAT、Workspace ID）。
 *
 * - serverUrl / workspaceId 存普通 SharedPreferences（不属于敏感数据）
 * - PAT 用 EncryptedSharedPreferences（AES256_GCM + AndroidKeyStore）
 */
class SettingsRepository(appContext: Context) {

    private val normal: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NORMAL, Context.MODE_PRIVATE)

    private val masterKey: MasterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encrypted: SharedPreferences = EncryptedSharedPreferences.create(
        appContext,
        PREFS_ENC,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state.asStateFlow()

    val current: Settings get() = _state.value

    fun update(transform: (Settings) -> Settings) {
        val s = transform(current)
        normal.edit()
            .putString(KEY_SERVER_URL, s.serverUrl)
            .putString(KEY_WORKSPACE_ID, s.workspaceId)
            .apply()
        encrypted.edit()
            .putString(KEY_PAT, s.pat)
            .apply()
        _state.value = s
    }

    private fun load(): Settings = Settings(
        serverUrl = normal.getString(KEY_SERVER_URL, null) ?: BuildConfig.DEFAULT_SERVER_URL,
        workspaceId = normal.getString(KEY_WORKSPACE_ID, null) ?: "",
        pat = encrypted.getString(KEY_PAT, null) ?: BuildConfig.DEFAULT_PAT,
    )

    data class Settings(
        val serverUrl: String,
        val workspaceId: String,
        val pat: String,
    ) {
        val isConfigured: Boolean
            get() = serverUrl.isNotBlank() && pat.isNotBlank()
    }

    companion object {
        private const val PREFS_NORMAL = "multica_settings"
        private const val PREFS_ENC = "multica_settings_secure"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_WORKSPACE_ID = "workspace_id"
        private const val KEY_PAT = "pat"
    }
}
