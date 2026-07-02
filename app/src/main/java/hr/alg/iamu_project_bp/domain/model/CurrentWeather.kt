package hr.alg.iamu_project_bp.domain.model

data class CurrentWeather(
    val temperatureC: Double,
    val weatherCode: Int,
    val windSpeedKmh: Double,
    val humidityPercent: Int,
    val updatedAtEpochMs: Long,
)
