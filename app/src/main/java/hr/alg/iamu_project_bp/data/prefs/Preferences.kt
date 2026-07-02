package hr.alg.iamu_project_bp.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import hr.alg.iamu_project_bp.R

fun Context.appPreferences(): SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(this)

fun Context.cityPreferences(cityId: Long): SharedPreferences =
    getSharedPreferences("city_prefs_$cityId", Context.MODE_PRIVATE)

const val NO_HOME_CITY_ID = -1L

const val DEFAULT_SYNC_INTERVAL_MINUTES = 60L

fun Context.getUnits(): String =
    appPreferences().getString(getString(R.string.pref_key_units), null)
        ?: getString(R.string.pref_units_value_metric)

fun Context.setUnits(units: String) = appPreferences().edit {
    putString(getString(R.string.pref_key_units), units)
}

fun Context.isMetric(): Boolean =
    getUnits() == getString(R.string.pref_units_value_metric)

fun Context.getSyncIntervalMinutes(): Long =
    appPreferences().getString(getString(R.string.pref_key_sync_interval), null)
        ?.toLongOrNull()
        ?: DEFAULT_SYNC_INTERVAL_MINUTES

fun Context.setSyncIntervalMinutes(minutes: Long) = appPreferences().edit {
    putString(getString(R.string.pref_key_sync_interval), minutes.toString())
}

fun Context.areNotificationsEnabled(): Boolean =
    appPreferences().getBoolean(getString(R.string.pref_key_notifications_enabled), true)

fun Context.setNotificationsEnabled(enabled: Boolean) = appPreferences().edit {
    putBoolean(getString(R.string.pref_key_notifications_enabled), enabled)
}

fun Context.getHomeCityId(): Long? =
    appPreferences()
        .getLongOr(getString(R.string.pref_key_home_city_id), NO_HOME_CITY_ID)
        .takeIf { it != NO_HOME_CITY_ID }

fun Context.setHomeCityId(cityId: Long?) = appPreferences().edit {
    putLong(getString(R.string.pref_key_home_city_id), cityId ?: NO_HOME_CITY_ID)
}

private const val KEY_CITY_LAST_VIEWED_AT = "last_viewed_at"

fun Context.markCityViewed(cityId: Long, atEpochMs: Long = System.currentTimeMillis()) =
    cityPreferences(cityId).edit { putLong(KEY_CITY_LAST_VIEWED_AT, atEpochMs) }

fun Context.getCityLastViewedAt(cityId: Long): Long? =
    cityPreferences(cityId).getLongOrNull(KEY_CITY_LAST_VIEWED_AT)

fun SharedPreferences.getLongOr(key: String, default: Long): Long =
    if (contains(key)) getLong(key, default) else default

fun SharedPreferences.getLongOrNull(key: String): Long? =
    if (contains(key)) getLong(key, 0L) else null
