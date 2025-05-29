package app.secondclass.healthsyncer.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import app.secondclass.healthsyncer.data.db.AppDatabase
import app.secondclass.healthsyncer.data.db.SessionDao
import app.secondclass.healthsyncer.data.db.SessionRepository
import app.secondclass.healthsyncer.data.strava.StravaActivityService
import app.secondclass.healthsyncer.data.strava.StravaApiClient
import app.secondclass.healthsyncer.util.StravaTokenManager
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Provides
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase =
        Room.databaseBuilder(app, AppDatabase::class.java, "fitbod.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    @Singleton
    fun provideSessionRepository(dao: SessionDao): SessionRepository =
        SessionRepository(dao)

    @Provides
    @Singleton
    fun provideStravaApiClient(
        app: Application,
        stravaActivityService: StravaActivityService,
        stravaTokenManager: StravaTokenManager
    ): StravaApiClient = StravaApiClient(app, stravaActivityService, stravaTokenManager)
}
