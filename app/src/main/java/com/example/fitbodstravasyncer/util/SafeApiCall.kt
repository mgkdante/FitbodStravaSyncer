package com.example.fitbodstravasyncer.util

import retrofit2.HttpException

suspend fun <T> safeStravaCall(
    call: suspend () -> T,
    onRateLimit: (isAppLimit: Boolean) -> Unit = {},
    onUnauthorized: () -> Unit = {},
    onOtherError: (Throwable) -> Unit = {}
): T? {
    return try {
        call()
    } catch (e: HttpException) {
        when (e.code()) {
            401, 403 -> onUnauthorized()
            429 -> {
                val appLimit = e.response()?.headers()?.get("X-RateLimit-AppLimit")
                val isAppLimit = !appLimit.isNullOrBlank()
                onRateLimit(isAppLimit)
            }
            else -> onOtherError(e)
        }
        null
    } catch (e: Throwable) {
        onOtherError(e)
        null
    }
}
