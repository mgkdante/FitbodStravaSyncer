package com.example.fitbodstravasyncer.core.network

import com.example.fitbodstravasyncer.App
import com.example.fitbodstravasyncer.data.strava.StravaConstants.BASE_URL
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitProvider {
    private val cacheSize = 5L * 1024 * 1024 // 5 MB
    private val cache = Cache(App.instance.cacheDir, cacheSize)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            // Cache GET requests for 3 minutes
            response.newBuilder()
                .header("Cache-Control", "public, max-age=900")
                .build()
        }
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
}