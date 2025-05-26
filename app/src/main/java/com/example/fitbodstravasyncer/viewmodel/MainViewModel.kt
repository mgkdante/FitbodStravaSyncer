package com.example.fitbodstravasyncer.viewmodel

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitbodstravasyncer.BuildConfig.STRAVA_CLIENT_ID
import com.example.fitbodstravasyncer.BuildConfig.STRAVA_CLIENT_SECRET
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.data.db.SessionEntity
import com.example.fitbodstravasyncer.data.db.SessionRepository
import com.example.fitbodstravasyncer.data.strava.StravaAuthService
import com.example.fitbodstravasyncer.feature.schedule.data.DailySyncScheduler
import com.example.fitbodstravasyncer.util.FitbodFetcher
import com.example.fitbodstravasyncer.util.StravaPrefs
import com.example.fitbodstravasyncer.worker.StravaUploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val KEY_FUTURE        = "future_auto_sync"
private const val KEY_DAILY         = "daily_sync_enabled"
private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo   = SessionRepository(AppDatabase.getInstance(application).sessionDao())
    private val prefs  = StravaPrefs.securePrefs(application)

    private val _uiState = MutableStateFlow(UiState(
        dynamicColor   = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        futureSync     = prefs.getBoolean(KEY_FUTURE, false),
        dailySync      = prefs.getBoolean(KEY_DAILY, false),
        stravaConnected = prefs.getString(StravaPrefs.KEY_ACCESS, null) != null
    ))
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadSessions()
    }

    private fun loadSessions() = viewModelScope.launch {
        repo.allSessions().collect { list ->
            Log.d("ViewModel", "Sessions from DB: " + list.joinToString { "${it.id} -> ${it.heartRateSeries}" })
            _uiState.update { it.copy(sessionMetrics = list.map { it.toMetrics() }) }
        }
    }

    /** Toggle whether to use dynamic (Material You) colors */
    fun toggleDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
        _uiState.update { it.copy(dynamicColor = enabled) }
    }

    fun deleteSessions(ids: List<String>) = viewModelScope.launch {
        repo.deleteSessions(ids)
    }

    fun deleteAllSessions() = viewModelScope.launch {
        repo.deleteAllSessions()
    }

    fun setDateFrom(date: LocalDate?) {
        _uiState.update { it.copy(dateFrom = date) }
    }

    fun setDateTo(date: LocalDate?) {
        _uiState.update { it.copy(dateTo = date) }
    }

    fun toggleFutureSync(enabled: Boolean) = viewModelScope.launch {
        prefs.edit { putBoolean(KEY_FUTURE, enabled) }
        _uiState.update { it.copy(futureSync = enabled) }
    }

    fun toggleDailySync(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DAILY, enabled) }
        if (enabled) DailySyncScheduler.schedule(getApplication())
        else       DailySyncScheduler.cancel(getApplication())
        _uiState.update { it.copy(dailySync = enabled) }
    }

    fun fetchSessions(from: LocalDate?, to: LocalDate?) = viewModelScope.launch {
        _uiState.update { it.copy(isFetching = true) }
        if (from == null || to == null) {
            _uiState.update { it.copy(isFetching = false) }
            return@launch
        }
        try {
            val start = from.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end   = to.atStartOfDay(ZoneId.systemDefault()).plusDays(1).toInstant()
            val healthClient = HealthConnectClient.getOrCreate(getApplication())

            // --- Fetch Strava activities for this window
            val token = "Bearer ${com.example.fitbodstravasyncer.util.StravaTokenManager.getValidAccessToken(getApplication())}"
            val stravaApi = com.example.fitbodstravasyncer.core.network.RetrofitProvider.retrofit.create(
                com.example.fitbodstravasyncer.data.strava.StravaActivityService::class.java
            )
            val stravaActivities = stravaApi.listActivities(token, 200, 1)
            // (Add paging here if your window is large!)

            // --- Fetch Fitbod sessions, matching to Strava activities
            val sessions = FitbodFetcher.fetchFitbodSessions(
                healthClient = healthClient,
                startInstant = start,
                endInstant = end,
                stravaActivities = stravaActivities
            )
            sessions.forEach { repo.saveSession(it) }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error fetching sessions", e)
        } finally {
            _uiState.update { it.copy(isFetching = false) }
        }
    }


    fun restoreStravaIds() = viewModelScope.launch {
        try {
            com.example.fitbodstravasyncer.data.strava.restoreStravaIds(getApplication())
        } catch (e: Exception) {
            Log.i("strava-log", e.toString())
        }
    }

    fun enqueueSyncAll() = viewModelScope.launch {
        _uiState.value.sessionMetrics
            .filter { it.stravaId == null }
            .forEach { StravaUploadWorker.enqueue(getApplication(), it.id) }
    }

    private val _isStravaConnected = MutableStateFlow(
        StravaPrefs.securePrefs(application).getString(StravaPrefs.KEY_ACCESS, null) != null
    )
    val isStravaConnected: StateFlow<Boolean> = _isStravaConnected

    // Call this after successful token exchange
    fun updateStravaConnectionState() {
        val connected = StravaPrefs.securePrefs(getApplication())
            .getString(StravaPrefs.KEY_ACCESS, null) != null
        _isStravaConnected.value = connected
    }

    // Modify your existing exchangeStravaCodeForToken to update state:
    suspend fun exchangeStravaCodeForTokenInViewModel(code: String) {
        val resp = StravaAuthService.create()
            .exchangeCode(STRAVA_CLIENT_ID, STRAVA_CLIENT_SECRET, code)
        StravaPrefs.securePrefs(getApplication()).edit(commit = true) {
            putString(StravaPrefs.KEY_ACCESS, resp.accessToken)
            putString(StravaPrefs.KEY_REFRESH, resp.refreshToken)
            putLong(StravaPrefs.KEY_EXPIRES, resp.expiresAt ?: 0L)
        }
        updateStravaConnectionState()
    }
}

private fun SessionEntity.toMetrics(): SessionMetrics =
    SessionMetrics(
        id            = id,
        title         = title,
        description   = description,
        dateTime      = dateTime,
        startTime     = startTime,
        activeTime    = activeTime,
        calories      = calories,
        heartRateSeries = heartRateSeries.sortedBy { it.time },
        avgHeartRate  = avgHeartRate?.toDouble(),
        stravaId      = stravaId
    )
