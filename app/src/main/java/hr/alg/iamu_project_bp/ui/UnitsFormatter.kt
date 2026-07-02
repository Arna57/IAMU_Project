package hr.alg.iamu_project_bp.ui

import android.content.Context
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.data.prefs.isMetric

object UnitsFormatter {
    fun temperature(context: Context, celsius: Double): String =
        if (context.isMetric()) {
            context.getString(R.string.format_temperature, celsius)
        } else {
            context.getString(R.string.format_temperature_f, toFahrenheit(celsius))
        }

    fun temperatureRange(context: Context, minCelsius: Double, maxCelsius: Double): String =
        if (context.isMetric()) {
            context.getString(R.string.format_temp_range, minCelsius, maxCelsius)
        } else {
            context.getString(
                R.string.format_temp_range,
                toFahrenheit(minCelsius),
                toFahrenheit(maxCelsius),
            )
        }

    fun wind(context: Context, kmh: Double): String =
        if (context.isMetric()) {
            context.getString(R.string.format_wind, kmh)
        } else {
            context.getString(R.string.format_wind_mph, kmh * KM_TO_MILES)
        }

    private fun toFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0

    private const val KM_TO_MILES = 0.621371
}
