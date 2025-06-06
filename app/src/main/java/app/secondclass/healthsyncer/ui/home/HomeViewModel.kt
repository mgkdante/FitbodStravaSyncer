package app.secondclass.healthsyncer.ui.home

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.secondclass.healthsyncer.BuildConfig
import app.secondclass.healthsyncer.data.db.AppDatabase
import app.secondclass.healthsyncer.data.db.SessionEntity
import app.secondclass.healthsyncer.data.db.SessionRepository
import app.secondclass.healthsyncer.data.fitbod.FitbodFetcher
import app.secondclass.healthsyncer.data.strava.StravaApiClient
import app.secondclass.healthsyncer.data.strava.StravaAuthService
import app.secondclass.healthsyncer.util.ApiRateLimitUtil
import app.secondclass.healthsyncer.util.NotificationHelper
import app.secondclass.healthsyncer.util.SessionMatcher
import app.secondclass.healthsyncer.util.SessionMetrics
import app.secondclass.healthsyncer.util.StravaPrefs
import app.secondclass.healthsyncer.util.UiState
import app.secondclass.healthsyncer.util.safeStravaCall
import app.secondclass.healthsyncer.worker.DailySyncScheduler
import app.secondclass.healthsyncer.worker.StravaAutoUploadWorker
import app.secondclass.healthsyncer.worker.StravaUploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val KEY_FUTURE = "future_auto_sync"
private const val KEY_DAILY = "daily_sync_enabled"
private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SessionRepository(AppDatabase.getInstance(application).sessionDao())
    private val prefs = StravaPrefs.securePrefs(application)
    private var lastCheckMatching = 0L

    sealed class SessionsUiState {
        object Loading : SessionsUiState()
        data class Content(val sessions: List<SessionMetrics>) : SessionsUiState()
        object Empty : SessionsUiState()
        data class Error(val message: String) : SessionsUiState()
    }

    private val _uiState = MutableStateFlow(
        UiState(
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            futureSync = prefs.getBoolean(KEY_FUTURE, false),
            dailySync = prefs.getBoolean(KEY_DAILY, false),
            stravaConnected = prefs.getString(StravaPrefs.KEY_ACCESS, null) != null,
            apiRequestsDay = StravaPrefs.getApiRequestCountDay(application),
            apiRequests15Min = StravaPrefs.getApiRequestCount15Min(application),
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds

    private val _sessionsUiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val sessionsUiState: StateFlow<SessionsUiState> = _sessionsUiState

    // --- THIS IS THE NEW STATE FOR NAVIGATION LOGIC ---
    private val _hasLocalSessions = MutableStateFlow(false)
    val hasLocalSessions: StateFlow<Boolean> = _hasLocalSessions

    val openAppIntent = NotificationHelper.createOpenAppIntent(getApplication())

    init {
        loadSessions()
        refreshApiCounters()
        updateUserApiUsageState()
    }

    private fun loadSessions() = viewModelScope.launch {
        try {
            repo.allSessions().collect { list ->
                // --- Update local session existence for navigation logic ---
                _hasLocalSessions.value = list.isNotEmpty()
                _sessionsUiState.value = when {
                    list.isEmpty() -> SessionsUiState.Empty
                    else -> SessionsUiState.Content(list.map { it.toMetrics() })
                }
                _uiState.update { it.copy(sessionMetrics = list.map { it.toMetrics() }) }
                checkAndWarnApiLimits()
            }
        } catch (e: Exception) {
            _sessionsUiState.value = SessionsUiState.Error(e.localizedMessage ?: "Unknown error loading sessions")
            Log.e("HomeViewModel", "Error loading sessions", e)
        }
    }

    private fun blockIfUserLimitReached(): Boolean {
        if (StravaPrefs.isUserApiLimitReached(getApplication())) {
            Toast.makeText(getApplication(), "You've hit your personal Strava API usage limit. Please try again in 15 minutes.", Toast.LENGTH_LONG).show()
            return true
        }
        return false
    }

    private fun refreshApiCounters() {
        val limitReached = isApiLimitReached()
        val resetHint = getApiResetTimeHint()
        _uiState.update {
            it.copy(
                apiRequestsDay = StravaPrefs.getApiRequestCountDay(getApplication()),
                apiRequests15Min = StravaPrefs.getApiRequestCount15Min(getApplication()),
                apiLimitReached = limitReached,
                apiLimitResetHint = resetHint
            )
        }
        checkAndWarnApiLimits()
    }

    private var lastWarnedAt = 0L

    private fun checkAndWarnApiLimits() {
        val context = getApplication<Application>()
        val now = System.currentTimeMillis()
        if (StravaPrefs.isUserApiLimitNear(context) && now - lastWarnedAt > 60 * 1000) {
            lastWarnedAt = now
            Toast.makeText(context, UiStrings.STRAVA_API_LIMIT_NEARLY_REACHED, Toast.LENGTH_LONG).show()
            NotificationHelper.showNotification(
                context,
                UiStrings.STRAVA_API_LIMIT_WARNING,
                UiStrings.STRAVA_API_LIMIT_BODY,
                9999,
                openAppIntent
            )
        }
        if (isApiLimitReached()) {
            Toast.makeText(context, "Strava API limit reached. Try again in ${getApiResetTimeHint()}.", Toast.LENGTH_LONG).show()
            NotificationHelper.showNotification(
                context,
                UiStrings.STRAVA_API_LIMIT_REACHED,
                "No more uploads until ${getApiResetTimeHint()}",
                10000,
                openAppIntent
            )
        }
    }

    fun isApiLimitReached(): Boolean {
        val context = getApplication<Application>()
        return StravaPrefs.getApiRequestCountDay(context) >= 2000 ||
                StravaPrefs.getApiRequestCount15Min(context) >= 200 ||
                StravaPrefs.getApiLimitReset(context) > System.currentTimeMillis()
    }

    fun getApiResetTimeHint(): String {
        return ApiRateLimitUtil.getApiResetTimeHint(getApplication())
    }

    fun canTriggerCheckMatching(): Boolean {
        val now = System.currentTimeMillis()
        return now - lastCheckMatching > 15 * 60 * 1000
    }

    fun triggerCheckMatching(action: suspend () -> Unit) {
        if (canTriggerCheckMatching()) {
            lastCheckMatching = System.currentTimeMillis()
            viewModelScope.launch {
                action()
                refreshApiCounters()
            }
        }
        checkAndWarnApiLimits()
    }

    fun selectAll(ids: List<String>) {
        _selectedIds.value = if (_selectedIds.value.containsAll(ids)) emptySet() else ids.toSet()
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
            StravaAutoUploadWorker.schedule(getApplication())
        } else {
            StravaAutoUploadWorker.cancel(getApplication())
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
        if (blockIfUserLimitReached()) return@launch
        _uiState.update { it.copy(isFetching = true) }
        if (from == null || to == null) {
            _uiState.update { it.copy(isFetching = false) }
            return@launch
        }
        try {
            val start = from.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = to.atStartOfDay(ZoneId.systemDefault()).plusDays(1).toInstant()
            val healthClient = HealthConnectClient.getOrCreate(getApplication())

            val sessions = FitbodFetcher.fetchFitbodSessionsWithStrava(
                context = getApplication(),
                healthClient = healthClient,
                startInstant = start,
                endInstant = end,
                toleranceSeconds = 300,
                onRateLimit = { isAppLimit ->
                    Toast.makeText(getApplication(), if (isAppLimit)
                        "App-wide Strava rate limit hit. Try again later."
                    else
                        "You've hit your Strava user rate limit. Wait and retry.", Toast.LENGTH_LONG).show()
                },
                onUnauthorized = {
                    Toast.makeText(getApplication(), "Strava authorization expired. Please reconnect.", Toast.LENGTH_LONG).show()
                },
                onOtherError = { e ->
                    Toast.makeText(getApplication(), "Strava/network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
            sessions.forEach { repo.saveSession(it) }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error fetching sessions", e)
        } finally {
            _uiState.update { it.copy(
                isFetching = false,
                hasFetchedOnce = true
            ) }
            refreshApiCounters()
        }
    }

    fun restoreStravaIds() = viewModelScope.launch {
        try {
            app.secondclass.healthsyncer.data.strava.restoreStravaIds(
                getApplication(),
                onRateLimit = { isAppLimit ->
                    Toast.makeText(getApplication(), if (isAppLimit)
                        "App-wide Strava rate limit hit. Try again later."
                    else
                        "You've hit your Strava user rate limit. Wait and retry.", Toast.LENGTH_LONG).show()
                },
                onUnauthorized = {
                    Toast.makeText(getApplication(), "Strava authorization expired. Please reconnect.", Toast.LENGTH_LONG).show()
                },
                onOtherError = { e ->
                    Toast.makeText(getApplication(), "Strava/network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        } catch (e: Exception) {
            Log.i("strava-log", e.toString())
        } finally {
            refreshApiCounters()
        }
    }

    fun unsyncIfStravaDeleted() = viewModelScope.launch {
        try {
            val client = StravaApiClient(getApplication())
            val dao = AppDatabase.getInstance(getApplication()).sessionDao()

            val stravaActivityIds = safeStravaCall(
                call = { client.listAllActivities().mapNotNull { it.id } },
                onRateLimit = { isAppLimit ->
                    Toast.makeText(getApplication(), if (isAppLimit)
                        "App-wide Strava rate limit hit. Try again later."
                    else
                        "You've hit your Strava user rate limit. Wait and retry.", Toast.LENGTH_LONG).show()
                },
                onUnauthorized = {
                    Toast.makeText(getApplication(), "Strava authorization expired. Please reconnect.", Toast.LENGTH_LONG).show()
                },
                onOtherError = { e ->
                    Toast.makeText(getApplication(), "Strava/network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            ) ?: return@launch

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
        } finally {
            refreshApiCounters()
        }
    }

    fun enqueueSyncAll() = viewModelScope.launch {
        if (blockIfUserLimitReached()) return@launch
        if (isApiLimitReached()) {
            Toast.makeText(getApplication(), "API limit reached. Try again in ${getApiResetTimeHint()}", Toast.LENGTH_LONG).show()
            return@launch
        }

        val client = StravaApiClient(getApplication())
        val recentActivities = safeStravaCall(
            call = { client.listAllActivities() },
            onRateLimit = { isAppLimit ->
                Toast.makeText(getApplication(), if (isAppLimit)
                    "App-wide Strava rate limit hit. Try again later."
                else
                    "You've hit your Strava user rate limit. Wait and retry.", Toast.LENGTH_LONG).show()
            },
            onUnauthorized = {
                Toast.makeText(getApplication(), "Strava authorization expired. Please reconnect.", Toast.LENGTH_LONG).show()
            },
            onOtherError = { e ->
                Toast.makeText(getApplication(), "Strava/network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        ) ?: return@launch

        _uiState.value.sessionMetrics
            .filter { it.stravaId == null }
            .forEach { session ->
                val sessionStartEpoch = session.startTime.epochSecond
                val tolerance = 300L // 5 min
                val matching = recentActivities.firstOrNull { activity ->
                    SessionMatcher.matchesSessionByTime(sessionStartEpoch, activity, tolerance)
                }
                if (matching != null) {
                    AppDatabase.getInstance(getApplication()).sessionDao()
                        .updateStravaId(session.id, matching.id)
                } else {
                    StravaUploadWorker.enqueue(getApplication(), session.id)
                }
            }
        refreshApiCounters()
    }

    private val _isStravaConnected = MutableStateFlow(
        StravaPrefs.securePrefs(application).getString(StravaPrefs.KEY_ACCESS, null) != null
    )

    fun updateStravaConnectionState() {
        val connected = StravaPrefs.securePrefs(getApplication())
            .getString(StravaPrefs.KEY_ACCESS, null) != null
        _isStravaConnected.value = connected
    }

    suspend fun exchangeStravaCodeForTokenInViewModel(code: String) {
        val resp = StravaAuthService.create()
            .exchangeCode(BuildConfig.STRAVA_CLIENT_ID, BuildConfig.STRAVA_CLIENT_SECRET, code)
        StravaPrefs.securePrefs(getApplication()).edit(commit = true) {
            putString(StravaPrefs.KEY_ACCESS, resp.accessToken)
            putString(StravaPrefs.KEY_REFRESH, resp.refreshToken)
            putLong(StravaPrefs.KEY_EXPIRES, resp.expiresAt ?: 0L)
        }
        updateStravaConnectionState()
        refreshApiCounters()
    }

    private fun updateUserApiUsageState() {
        val ctx = getApplication<Application>()
        val reads15m = StravaPrefs.getUserApiReadCount15Min(ctx)
        val reqs15m = StravaPrefs.getUserApiRequestCount15Min(ctx)
        val readsDay = StravaPrefs.getUserApiReadCountDay(ctx)
        val reqsDay = StravaPrefs.getApiRequestCountDay(ctx)

        val usageString = "Usage: $reads15m/90 reads (15m), $reqs15m/180 requests (15m), $readsDay/900 reads (day), $reqsDay/1800 requests (day)"
        val warning = StravaPrefs.isUserApiLimitNear(ctx)
        _uiState.update { it.copy(userApiUsageString = usageString, userApiWarning = warning) }
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
