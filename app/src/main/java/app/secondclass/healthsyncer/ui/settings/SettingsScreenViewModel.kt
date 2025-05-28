import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.secondclass.healthsyncer.ui.settings.AppThemeMode
import app.secondclass.healthsyncer.util.StravaPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.edit


class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = getApplication<Application>()

    private val _appThemeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode

    private val _isStravaConnected = MutableStateFlow(isStravaConnected())
    val isStravaConnected: StateFlow<Boolean> = _isStravaConnected

    private val _apiUsageString = MutableStateFlow(getApiUsageString())
    val apiUsageString: StateFlow<String> = _apiUsageString

    private val _userApiWarning = MutableStateFlow(StravaPrefs.isUserApiLimitNear(ctx))
    val userApiWarning: StateFlow<Boolean> = _userApiWarning

    private val _dynamicColorEnabled = MutableStateFlow(
        StravaPrefs.securePrefs(ctx).getBoolean("dynamic_color_enabled", true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled

    fun setDynamicColorEnabled(enabled: Boolean) {
        StravaPrefs.securePrefs(ctx).edit { putBoolean("dynamic_color_enabled", enabled) }
        _dynamicColorEnabled.value = enabled
    }


    fun setAppThemeMode(mode: AppThemeMode) { _appThemeMode.value = mode }

    fun refreshState() {
        _isStravaConnected.value = isStravaConnected()
        _apiUsageString.value = getApiUsageString()
        _userApiWarning.value = StravaPrefs.isUserApiLimitNear(ctx)
    }

    fun disconnectStrava() {
        StravaPrefs.disconnect(ctx)
        refreshState()
    }

    private fun isStravaConnected(): Boolean {
        val prefs = StravaPrefs.securePrefs(ctx)
        return !prefs.getString(StravaPrefs.KEY_ACCESS, null).isNullOrBlank() &&
                !prefs.getString(StravaPrefs.KEY_REFRESH, null).isNullOrBlank()
    }

    private fun getApiUsageString(): String {
        val reads15m = StravaPrefs.getUserApiReadCount15Min(ctx)
        val reqs15m  = StravaPrefs.getUserApiRequestCount15Min(ctx)
        val readsDay = StravaPrefs.getUserApiReadCountDay(ctx)
        val reqsDay  = StravaPrefs.getUserApiRequestCountDay(ctx)
        return "Usage: $reads15m/90 reads (15m), $reqs15m/180 requests (15m), " +
                "$readsDay/900 reads (day), $reqsDay/1800 requests (day)"
    }
}
