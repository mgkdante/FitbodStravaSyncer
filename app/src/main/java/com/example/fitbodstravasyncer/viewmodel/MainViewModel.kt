package com.example.fitbodstravasyncer.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.data.db.SessionEntity
import com.example.fitbodstravasyncer.data.db.SessionRepository
import com.example.fitbodstravasyncer.feature.schedule.data.DailySyncScheduler
import com.example.fitbodstravasyncer.util.FitbodFetcher
import com.example.fitbodstravasyncer.util.StravaPrefs
import com.example.fitbodstravasyncer.worker.StravaAutoUploadWorker
import com.example.fitbodstravasyncer.worker.StravaUploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import androidx.core.content.edit

private const val KEY_FUTURE = "future_auto_sync"
private const val KEY_DAILY = "daily_sync_enabled"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: SessionRepository = SessionRepository(AppDatabase.getInstance(application).sessionDao())
    private val prefs = StravaPrefs.securePrefs(application)

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadSessions()
        initToggles()
    }

    private fun loadSessions() = viewModelScope.launch {
        repo.allSessions().collect { list: List<SessionEntity> ->
            _uiState.update { state ->
                state.copy(sessionMetrics = list.map { it.toMetrics() })
            }
        }
    }

    fun deleteSessions(ids: List<String>) = viewModelScope.launch {
        repo.deleteSessions(ids)
    }

    fun deleteAllSessions() = viewModelScope.launch {
        repo.deleteAllSessions()
    }

    private fun initToggles() {
        _uiState.update { state ->
            state.copy(
                futureSync = prefs.getBoolean(KEY_FUTURE, false),
                dailySync = prefs.getBoolean(KEY_DAILY, false),
                stravaConnected = prefs.getString(StravaPrefs.KEY_ACCESS, null) != null
            )
        }
    }

    fun setDateFrom(date: LocalDate?) {
        _uiState.update { it.copy(dateFrom = date) }
    }
    fun setDateTo(date: LocalDate?) {
        _uiState.update { it.copy(dateTo = date) }
    }

    fun toggleFutureSync(enabled: Boolean) = viewModelScope.launch {
        prefs.edit { putBoolean(KEY_FUTURE, enabled) }
        if (enabled) StravaAutoUploadWorker.schedule(getApplication())
        else StravaAutoUploadWorker.cancel(getApplication())
        _uiState.update { it.copy(futureSync = enabled) }
    }

    fun toggleDailySync(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DAILY, enabled) }
        if (enabled) DailySyncScheduler.schedule(getApplication())
        else DailySyncScheduler.cancel(getApplication())
        _uiState.update { it.copy(dailySync = enabled) }
    }

    fun fetchSessions(from: LocalDate?, to: LocalDate?) = viewModelScope.launch {
        Log.d("FETCH-LOG", "Fetch-Log STARTED: from=$from, to=$to")
        _uiState.update { it.copy(isFetching = true) }
        try {
            if (from == null || to == null) {
                Log.d("FETCH-LOG", "Dates null, setting isFetching=false and returning")
                return@launch
            }
            val start = from.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = to.atStartOfDay(ZoneId.systemDefault()).plusDays(1).toInstant()
            Log.d("FETCH-LOG", "About to call fetchFitbodSessions()")
            val sessions = FitbodFetcher.fetchFitbodSessions(
                androidx.health.connect.client.HealthConnectClient.getOrCreate(getApplication()),
                start, end
            )
            Log.d("FETCH-LOG", "Got ${sessions.size} sessions. Saving to repo...")
            sessions.forEach { session -> repo.saveSession(session) }
            try {
                Log.d("FETCH-LOG", "Restoring Strava IDs")
                com.example.fitbodstravasyncer.data.strava.restoreStravaIds(getApplication())
            } catch (restoreEx: Exception) {
                Log.e("FETCH-LOG", "Error restoring Strava IDs: ${restoreEx.message}", restoreEx)
            }
            Log.d("FETCH-LOG", "Finished fetchSessions try block")
        } catch (e: Exception) {
            Log.e("FETCH-LOG", "Exception! ${e.message}", e)
        } finally {
            Log.d("FETCH-LOG", "Setting isFetching = FALSE")
            _uiState.update { it.copy(isFetching = false) }
        }
    }






    fun restoreStravaIds() = viewModelScope.launch {
        try { com.example.fitbodstravasyncer.data.strava.restoreStravaIds(getApplication()) } catch (_: Exception) {}
    }

    fun enqueueSyncAll() = viewModelScope.launch {
        _uiState.value.sessionMetrics
            .filter { it.stravaId == null }
            .forEach { StravaUploadWorker.enqueue(getApplication(), it.id) }
    }
}

// Extension to map entity to metrics
private fun SessionEntity.toMetrics(): SessionMetrics =
    SessionMetrics(
        id = id,
        title = title,
        dateTime = dateTime,
        startTime = startTime,
        activeTime = activeTime,
        calories = calories,
        avgHeartRate = avgHeartRate?.toDouble(),
        stravaId = stravaId
    )
