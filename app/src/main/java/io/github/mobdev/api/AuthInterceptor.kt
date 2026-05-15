package io.github.mobdev.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    @Volatile
    var token: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = token?.let { t ->
            chain.request().newBuilder()
                .addHeader("X-Auth-Token", t)
                .build()
        } ?: chain.request()
        return chain.proceed(request)
    }
}
