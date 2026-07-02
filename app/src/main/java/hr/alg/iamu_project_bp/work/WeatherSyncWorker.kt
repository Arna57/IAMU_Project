package hr.alg.iamu_project_bp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import hr.alg.iamu_project_bp.AppGraph
import hr.alg.iamu_project_bp.domain.model.City
import hr.alg.iamu_project_bp.notification.NotificationHelper
import java.io.IOException

class WeatherSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    companion object {
        const val WORK_NAME = "weather_sync"

        const val KEY_CITY_ID = "city_id"
    }

    override suspend fun doWork(): Result {
        val cityRepository = AppGraph.cityRepository(applicationContext)
        val weatherRepository = AppGraph.weatherRepository(applicationContext)

        val requestedCityId = inputData.getLong(KEY_CITY_ID, City.NEW_ID)
        val cities = if (requestedCityId != City.NEW_ID) {
            listOfNotNull(cityRepository.getById(requestedCityId))
        } else {
            cityRepository.getAll()
        }

        return try {
            cities.forEach { city -> weatherRepository.refresh(city.id) }
            if (cities.isNotEmpty()) {
                NotificationHelper(applicationContext).showSyncComplete()
            }
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        }
    }
}
