package hr.alg.iamu_project_bp.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import hr.alg.iamu_project_bp.MainActivity
import hr.alg.iamu_project_bp.R

class NotificationHelper(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService<NotificationManager>()!!

    init {
        createChannels()
    }

    private fun createChannels() {
        val alerts = NotificationChannel(
            appContext.getString(R.string.notification_channel_id_alerts),
            appContext.getString(R.string.notification_channel_name_alerts),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.notification_channel_description_alerts)
        }
        val sync = NotificationChannel(
            appContext.getString(R.string.notification_channel_id_sync),
            appContext.getString(R.string.notification_channel_name_sync),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = appContext.getString(R.string.notification_channel_description_sync)
        }
        notificationManager.createNotificationChannel(alerts)
        notificationManager.createNotificationChannel(sync)
    }

    fun showWeatherAlert(cityId: Long, cityName: String, message: String) {
        if (!notificationsAllowed()) return

        val title = appContext.getString(R.string.notification_alert_title, cityName)
        val text = message.ifBlank { appContext.getString(R.string.notification_sync_text) }
        val id = ALERT_NOTIFICATION_ID_BASE + cityId.toInt()

        val notification = baseBuilder(
            channelId = appContext.getString(R.string.notification_channel_id_alerts),
            iconRes = R.drawable.ic_weather_storm,
            title = title,
            text = text,
            requestCode = id,
        )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()

        post(id, notification)
    }

    fun showSyncComplete() {
        if (!notificationsAllowed()) return

        val notification = baseBuilder(
            channelId = appContext.getString(R.string.notification_channel_id_sync),
            iconRes = R.drawable.ic_weather_cloud,
            title = appContext.getString(R.string.notification_sync_title),
            text = appContext.getString(R.string.notification_sync_text),
            requestCode = SYNC_NOTIFICATION_ID,
        )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        post(SYNC_NOTIFICATION_ID, notification)

        DelayedExecutor.postDelayed(SYNC_AUTO_DISMISS_MS, token = SYNC_NOTIFICATION_ID) {
            notificationManager.cancel(SYNC_NOTIFICATION_ID)
        }
    }

    private fun baseBuilder(
        channelId: String,
        iconRes: Int,
        title: String,
        text: String,
        requestCode: Int,
    ): NotificationCompat.Builder {
        val contentIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            requestCode,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }

    private fun post(id: Int, notification: android.app.Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun notificationsAllowed(): Boolean =
        preferenceEnabled() && hasPostPermission()

    private fun preferenceEnabled(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        val key = appContext.getString(R.string.pref_key_notifications_enabled)
        return prefs.getBoolean(key, true)
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val SYNC_NOTIFICATION_ID = 1001

        const val ALERT_NOTIFICATION_ID_BASE = 2000

        private const val SYNC_AUTO_DISMISS_MS = 8_000L
    }
}
