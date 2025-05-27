package com.example.fitbodstravasyncer.data.strava

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

/* ---------- DTO ---------- */
data class TokenResponse(
    @Json(name = "access_token")  val accessToken:  String?,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "expires_at")    val expiresAt:    Long?
)

/* ---------- API ---------- */
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
        fun create(): StravaAuthService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://www.strava.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(StravaAuthService::class.java)
        }
    }

}
