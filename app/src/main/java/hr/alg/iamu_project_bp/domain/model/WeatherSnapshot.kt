package hr.alg.iamu_project_bp.domain.model

data class WeatherSnapshot(
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
)
