package app.secondclass.healthsyncer.data.strava


import app.secondclass.healthsyncer.BuildConfig

object StravaConstants {
    const val CLIENT_ID     = BuildConfig.STRAVA_CLIENT_ID
    const val CLIENT_SECRET = BuildConfig.STRAVA_CLIENT_SECRET
    const val REDIRECT_URI  = "myapp://strava-auth"
    const val AUTH_URL      = "https://www.strava.com/oauth/authorize"
    const val BASE_URL      = "https://www.strava.com/api/v3/"
}
