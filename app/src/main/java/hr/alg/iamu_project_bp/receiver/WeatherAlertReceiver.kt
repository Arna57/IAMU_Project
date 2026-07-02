package hr.alg.iamu_project_bp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hr.alg.iamu_project_bp.AppGraph
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.model.City
import hr.alg.iamu_project_bp.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherAlertReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_WEATHER_ALERT = "hr.alg.iamu_project_bp.ACTION_WEATHER_ALERT"

        const val EXTRA_CITY_ID = "hr.alg.iamu_project_bp.EXTRA_CITY_ID"

        const val EXTRA_MESSAGE = "hr.alg.iamu_project_bp.EXTRA_MESSAGE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WEATHER_ALERT) return

        val appContext = context.applicationContext
        val cityId = intent.getLongExtra(EXTRA_CITY_ID, City.NEW_ID)
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cityName = resolveCityName(appContext, cityId)
                NotificationHelper(appContext).showWeatherAlert(cityId, cityName, message)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun resolveCityName(context: Context, cityId: Long): String {
        val fallback = context.getString(R.string.app_name)
        if (cityId == City.NEW_ID) return fallback
        return try {
            AppGraph.cityRepository(context).getById(cityId)?.name ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
