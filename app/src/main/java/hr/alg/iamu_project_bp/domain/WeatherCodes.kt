package hr.alg.iamu_project_bp.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import hr.alg.iamu_project_bp.R

object WeatherCodes {
    @StringRes
    fun descriptionRes(code: Int): Int = when (code) {
        0 -> R.string.weather_clear
        1 -> R.string.weather_mainly_clear
        2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_overcast
        45, 48 -> R.string.weather_fog
        51, 53, 55 -> R.string.weather_drizzle
        56, 57 -> R.string.weather_freezing_drizzle
        61, 63, 65 -> R.string.weather_rain
        66, 67 -> R.string.weather_freezing_rain
        71, 73, 75 -> R.string.weather_snow
        77 -> R.string.weather_snow_grains
        80, 81, 82 -> R.string.weather_rain_showers
        85, 86 -> R.string.weather_snow_showers
        95 -> R.string.weather_thunderstorm
        96, 99 -> R.string.weather_thunderstorm_hail
        else -> R.string.weather_unknown
    }

    @DrawableRes
    fun iconRes(code: Int): Int = when (code) {
        0, 1 -> R.drawable.ic_weather_sun
        2, 3 -> R.drawable.ic_weather_cloud
        45, 48 -> R.drawable.ic_weather_fog
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_weather_rain
        71, 73, 75, 77, 85, 86 -> R.drawable.ic_weather_snow
        95, 96, 99 -> R.drawable.ic_weather_storm
        else -> R.drawable.ic_weather_cloud
    }
}
