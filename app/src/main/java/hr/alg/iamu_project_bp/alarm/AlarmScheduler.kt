package hr.alg.iamu_project_bp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import hr.alg.iamu_project_bp.receiver.WeatherAlertReceiver

object AlarmScheduler {
    private const val INEXACT_WINDOW_MS = 10 * 60 * 1000L

    fun schedule(context: Context, cityId: Long, message: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = alarmPendingIntent(context, cityId, message)

        if (canScheduleExact(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent,
            )
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, INEXACT_WINDOW_MS, pendingIntent,
            )
        }
    }

    fun cancel(context: Context, cityId: Long) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = alarmPendingIntent(context, cityId, message = null)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmPendingIntent(
        context: Context,
        cityId: Long,
        message: String?,
    ): PendingIntent {
        val intent = Intent(context, WeatherAlertReceiver::class.java).apply {
            action = WeatherAlertReceiver.ACTION_WEATHER_ALERT

            setPackage(context.packageName)
            putExtra(WeatherAlertReceiver.EXTRA_CITY_ID, cityId)
            if (message != null) {
                putExtra(WeatherAlertReceiver.EXTRA_MESSAGE, message)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            cityId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
