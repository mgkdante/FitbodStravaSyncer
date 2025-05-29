package app.secondclass.healthsyncer.di

import android.content.Context
import app.secondclass.healthsyncer.data.strava.StravaActivityService
import app.secondclass.healthsyncer.data.strava.StravaAuthService
import app.secondclass.healthsyncer.data.strava.StravaConstants
import dagger.Module
import dagger.Provides
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext app: Context): OkHttpClient =
        OkHttpClient.Builder()
            .cache(Cache(app.cacheDir, 5L * 1024 * 1024))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder().header("Cache-Control", "public, max-age=900").build()
            }
            .build()

    @Provides @Singleton @Named("ApiRetrofit")
    fun provideApiRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(StravaConstants.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton @Named("AuthRetrofit")
    fun provideAuthRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.strava.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    fun provideStravaActivityService(@Named("ApiRetrofit") retrofit: Retrofit): StravaActivityService =
        retrofit.create(StravaActivityService::class.java)

    @Provides
    fun provideStravaAuthService(@Named("AuthRetrofit") retrofit: Retrofit): StravaAuthService =
        retrofit.create(StravaAuthService::class.java)
}
