package com.multica.app.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object MulticaApiFactory {

    /** 宽容的 JSON：缺字段不抛、未知字段忽略、字段名容忍大小写。 */
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    fun build(
        baseUrl: String,
        authToken: String?,
        okHttp: OkHttpClient? = null,
    ): MulticaApi {
        val safeBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val client = (okHttp ?: defaultOkHttp()).newBuilder()
            .addInterceptor(AuthInterceptor(authToken))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(safeBase)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(MulticaApi::class.java)
    }

    fun defaultOkHttp(): OkHttpClient {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            // pingInterval 禁用 — multica server 不实现 WS pong handler，
            // OkHttp 发 ping 后 server 不回 pong，会被 OkHttp 当连接失败主动 close。
            // 改在 MulticaWebSocket 里用应用层 text message `{"type":"ping"}` 做 keepalive。
            .pingInterval(0, TimeUnit.SECONDS)
            .addInterceptor(log)
            .build()
    }

    /** 构建 GitHub API 实例（无需 auth） */
    fun buildGitHub(): GitHubApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(GitHubApi::class.java)
    }
}
