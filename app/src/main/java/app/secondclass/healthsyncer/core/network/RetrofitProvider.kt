package app.secondclass.healthsyncer.core.network

import app.secondclass.healthsyncer.App
import app.secondclass.healthsyncer.data.strava.StravaConstants.BASE_URL
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
            response.newBuilder()
                .header("Cache-Control", "public, max-age=900") // 3 min
                .build()
        }
        .build()

    // for all /api/v3/ endpoints (activities, uploads…)
    private val apiRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // for OAuth endpoints (/oauth/token)
    private val authRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://www.strava.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Use for API v3 calls (StravaActivityService, etc).
     */
    fun <T> createApiService(clazz: Class<T>): T =
        apiRetrofit.create(clazz)

    /**
     * Use for authentication (/oauth/token).
     */
    fun <T> createAuthService(clazz: Class<T>): T =
        authRetrofit.create(clazz)
}
