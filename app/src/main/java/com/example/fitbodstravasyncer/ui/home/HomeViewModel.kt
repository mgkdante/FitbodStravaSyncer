package com.example.fitbodstravasyncer.ui.home

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitbodstravasyncer.BuildConfig
import com.example.fitbodstravasyncer.core.network.RetrofitProvider
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.data.db.SessionEntity
import com.example.fitbodstravasyncer.data.db.SessionRepository
import com.example.fitbodstravasyncer.data.fitbod.FitbodFetcher
import com.example.fitbodstravasyncer.data.strava.StravaActivityResponse
import com.example.fitbodstravasyncer.data.strava.StravaActivityService
import com.example.fitbodstravasyncer.data.strava.StravaApiClient
import com.example.fitbodstravasyncer.data.strava.StravaAuthService
import com.example.fitbodstravasyncer.util.SessionMetrics
import com.example.fitbodstravasyncer.util.StravaPrefs
import com.example.fitbodstravasyncer.util.StravaTokenManager
import com.example.fitbodstravasyncer.util.UiState
import com.example.fitbodstravasyncer.worker.DailySyncScheduler
import com.example.fitbodstravasyncer.worker.StravaAutoUploadWorker
import com.example.fitbodstravasyncer.worker.StravaUploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private const val KEY_FUTURE = "future_auto_sync"
private const val KEY_DAILY = "daily_sync_enabled"
private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo =
        SessionRepository(AppDatabase.Companion.getInstance(application).sessionDao())
    private val prefs = StravaPrefs.securePrefs(application)

    private val _uiState = MutableStateFlow(
        UiState(
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            futureSync = prefs.getBoolean(KEY_FUTURE, false),
            dailySync = prefs.getBoolean(KEY_DAILY, false),
            stravaConnected = prefs.getString(StravaPrefs.KEY_ACCESS, null) != null
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    // In MainViewModel.kt

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds


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

    fun toggleSelection(id: String) {
        _selectedIds.value = if (_selectedIds.value.contains(id))
            _selectedIds.value - id
        else
            _selectedIds.value + id
    }

    fun toggleExpansion(id: String) {
        _expandedIds.value = if (_expandedIds.value.contains(id))
            _expandedIds.value - id
        else
            _expandedIds.value + id
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
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
        if (enabled) {
            StravaAutoUploadWorker.Companion.schedule(getApplication())
        } else {
            StravaAutoUploadWorker.Companion.cancel(getApplication())
        }
        _uiState.update { it.copy(futureSync = enabled) }
    }

    fun toggleDailySync(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DAILY, enabled) }
        if (enabled) DailySyncScheduler.schedule(getApplication())
        else DailySyncScheduler.cancel(getApplication())
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

            // --- Fetch ALL Strava activities in this window ---
            val token   = "Bearer ${StravaTokenManager.getValidAccessToken(getApplication())}"
            val stravaApi = RetrofitProvider.createApiService(StravaActivityService::class.java)

            val stravaActivities = mutableListOf<StravaActivityResponse>()
            var page = 1
            while (true) {
                val batch = stravaApi.listActivities(
                    auth    = token,
                    perPage = 200,
                    page    = page
                )
                if (batch.isEmpty()) break
                stravaActivities.addAll(batch)
                page++
            }

            // --- Fetch Fitbod sessions and match them ---
            val sessions = FitbodFetcher.fetchFitbodSessions(
                healthClient     = healthClient,
                startInstant     = start,
                endInstant       = end,
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

    /** Check all synced workouts and unsync if deleted on Strava */
    fun unsyncIfStravaDeleted() = viewModelScope.launch {
        try {
            val client = StravaApiClient(getApplication())
            val dao = AppDatabase.getInstance(getApplication()).sessionDao()

            val stravaActivityIds = client.listAllActivities().mapNotNull { it.id }.toSet()

            val sessionsWithStravaId = dao.getAllOnce().filter { it.stravaId != null }
            var unsyncedCount = 0

            for (session in sessionsWithStravaId) {
                val stravaId = session.stravaId
                if (stravaId != null && stravaId !in stravaActivityIds) {
                    dao.updateStravaId(session.id, null)
                    unsyncedCount++
                    Log.i("strava-sync", "Unsynced ${session.id} (deleted on Strava)")
                }
            }
            Log.i("strava-sync", "Unsynced $unsyncedCount workouts that were deleted on Strava.")
        } catch (e: Exception) {
            Log.e("strava-sync", "Failed to unsync deleted Strava workouts: ${e.message}")
        }
    }


    fun enqueueSyncAll() = viewModelScope.launch {
        val client = StravaApiClient(getApplication())
        val recentActivities = client.listAllActivities()

        _uiState.value.sessionMetrics
            .filter { it.stravaId == null }
            .forEach { session ->
                val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)
                val sessionStartEpoch = session.startTime.epochSecond
                val tolerance = 300L // 5 min
                val matching = recentActivities.firstOrNull { activity ->
                    activity.startDate?.let {
                        val actEpoch = Instant.from(formatter.parse(it)).epochSecond
                        abs(actEpoch - sessionStartEpoch) < tolerance
                    } == true
                }
                if (matching != null) {
                    AppDatabase.getInstance(getApplication()).sessionDao()
                        .updateStravaId(session.id, matching.id)
                } else {
                    StravaUploadWorker.enqueue(getApplication(), session.id)
                }
            }
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
        val resp = StravaAuthService.Companion.create()
            .exchangeCode(BuildConfig.STRAVA_CLIENT_ID, BuildConfig.STRAVA_CLIENT_SECRET, code)
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
        id = id,
        title = title,
        description = description,
        dateTime = dateTime,
        startTime = startTime,
        activeTime = activeTime,
        calories = calories,
        heartRateSeries = heartRateSeries.sortedBy { it.time },
        avgHeartRate = avgHeartRate?.toDouble(),
        stravaId = stravaId
    )