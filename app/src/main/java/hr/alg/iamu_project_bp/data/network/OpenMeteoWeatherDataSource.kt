package hr.alg.iamu_project_bp.data.network

import hr.alg.iamu_project_bp.domain.model.WeatherSnapshot
import hr.alg.iamu_project_bp.domain.repository.WeatherDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class OpenMeteoWeatherDataSource(
    private val api: ForecastApi,
) : WeatherDataSource {
    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
    ): WeatherSnapshot = withContext(Dispatchers.IO) {
        val dto = try {
            api.getForecast(latitude = latitude, longitude = longitude)
        } catch (e: HttpException) {
            throw IOException("Weather request failed with HTTP ${e.code()}", e)
        }
        WeatherResponseParser.toSnapshot(dto)
    }
}
