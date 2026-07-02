package hr.alg.iamu_project_bp.domain.repository

import hr.alg.iamu_project_bp.domain.model.WeatherSnapshot

interface WeatherDataSource {
    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherSnapshot
}
