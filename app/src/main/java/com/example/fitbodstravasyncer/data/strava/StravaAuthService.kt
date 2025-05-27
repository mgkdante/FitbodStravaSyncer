package com.example.fitbodstravasyncer.data.strava

import com.example.fitbodstravasyncer.core.network.RetrofitProvider
import com.squareup.moshi.Json
import retrofit2.http.*

data class TokenResponse(
    @Json(name = "access_token")  val accessToken:  String?,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "expires_at")    val expiresAt:    Long?
)

interface StravaAuthService {

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun exchangeCode(
        @Field("client_id")     clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code")          code: String,
        @Field("grant_type")    grantType: String = "authorization_code"
    ): TokenResponse

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun refreshToken(
        @Field("client_id")     clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type")    grantType: String = "refresh_token"
    ): TokenResponse

    companion object {
        fun create(): StravaAuthService =
            RetrofitProvider.createAuthService(StravaAuthService::class.java)
    }
}
