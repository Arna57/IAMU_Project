package hr.alg.iamu_project_bp.domain.model

data class DailyForecast(
    val dateIso: String,
    val minTempC: Double,
    val maxTempC: Double,
    val weatherCode: Int,
    val precipitationMm: Double,
    val windSpeedKmh: Double,
)
