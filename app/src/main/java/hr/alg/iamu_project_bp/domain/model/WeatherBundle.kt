package hr.alg.iamu_project_bp.domain.model

data class WeatherBundle(
    val city: City,
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
)
