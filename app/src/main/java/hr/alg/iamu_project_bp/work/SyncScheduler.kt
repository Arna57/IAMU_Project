package hr.alg.iamu_project_bp.work

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.model.City
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val DEFAULT_SYNC_INTERVAL_MINUTES = 60L

    fun schedulePeriodic(context: Context) {
        val intervalMinutes = readSyncIntervalMinutes(context)
        val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            intervalMinutes, TimeUnit.MINUTES,
        )
            .setConstraints(connectedConstraint())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeatherSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun reschedule(context: Context) {
        val intervalMinutes = readSyncIntervalMinutes(context)
        val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            intervalMinutes, TimeUnit.MINUTES,
        )
            .setConstraints(connectedConstraint())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeatherSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun triggerOneShotSync(context: Context, cityId: Long? = null) {
        val builder = OneTimeWorkRequestBuilder<WeatherSyncWorker>()
            .setConstraints(connectedConstraint())
        if (cityId != null && cityId != City.NEW_ID) {
            builder.setInputData(
                Data.Builder()
                    .putLong(WeatherSyncWorker.KEY_CITY_ID, cityId)
                    .build(),
            )
        }
        WorkManager.getInstance(context).enqueue(builder.build())
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WeatherSyncWorker.WORK_NAME)
    }

    private fun connectedConstraint(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    private fun readSyncIntervalMinutes(context: Context): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = context.getString(R.string.pref_key_sync_interval)

        val raw = prefs.getString(key, null)
        return raw?.toLongOrNull() ?: DEFAULT_SYNC_INTERVAL_MINUTES
    }
}
