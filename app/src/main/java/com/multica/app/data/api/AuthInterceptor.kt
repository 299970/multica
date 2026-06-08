package com.multica.app.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 把 PAT 加到 Authorization: Bearer <token>。
 * 没配置 token 时不动请求头，让服务端返回 401，触发 APP 引导到设置页。
 */
class AuthInterceptor(
    @Volatile private var token: String?,
) : Interceptor {

    fun setToken(newToken: String?) {
        token = newToken?.takeIf { it.isNotBlank() }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val t = token
        return if (t.isNullOrBlank()) chain.proceed(req)
        else chain.proceed(
            req.newBuilder()
                .header("Authorization", "Bearer $t")
                .header("Accept", "application/json")
                .build()
        )
    }
}
