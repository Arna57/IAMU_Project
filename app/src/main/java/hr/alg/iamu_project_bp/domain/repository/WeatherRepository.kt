package hr.alg.iamu_project_bp.domain.repository

import hr.alg.iamu_project_bp.domain.model.WeatherBundle

interface WeatherRepository {
    suspend fun getCached(cityId: Long): WeatherBundle?

    suspend fun refresh(cityId: Long): WeatherBundle
}
