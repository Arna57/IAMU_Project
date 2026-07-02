package hr.alg.iamu_project_bp.data.network.dto

import com.google.gson.annotations.SerializedName

data class ForecastResponseDto(
    @SerializedName("current") val current: CurrentDto?,
    @SerializedName("daily") val daily: DailyDto?,
) {
    data class CurrentDto(
        @SerializedName("temperature_2m") val temperature2m: Double?,
        @SerializedName("relative_humidity_2m") val relativeHumidity2m: Int?,
        @SerializedName("weather_code") val weatherCode: Int?,
        @SerializedName("wind_speed_10m") val windSpeed10m: Double?,
    )

    data class DailyDto(
        @SerializedName("time") val time: List<String>?,
        @SerializedName("weather_code") val weatherCode: List<Int>?,
        @SerializedName("temperature_2m_min") val temperatureMin: List<Double>?,
        @SerializedName("temperature_2m_max") val temperatureMax: List<Double>?,
        @SerializedName("precipitation_sum") val precipitationSum: List<Double>?,
        @SerializedName("wind_speed_10m_max") val windSpeedMax: List<Double>?,
    )
}
