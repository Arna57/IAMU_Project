package hr.alg.iamu_project_bp.data.network

import hr.alg.iamu_project_bp.data.network.dto.ForecastResponseDto
import hr.alg.iamu_project_bp.data.network.dto.GeocodingResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
    ): ForecastResponseDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        const val CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"

        const val DAILY_FIELDS =
            "weather_code,temperature_2m_min,temperature_2m_max," +
                "precipitation_sum,wind_speed_10m_max"
    }
}

interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") name: String,
        @Query("count") count: Int = DEFAULT_COUNT,
        @Query("language") language: String = "en",
    ): GeocodingResponseDto

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"
        const val DEFAULT_COUNT = 10
    }
}
