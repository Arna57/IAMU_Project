package hr.alg.iamu_project_bp.data.network

import hr.alg.iamu_project_bp.data.network.dto.GeocodingResponseDto.ResultDto
import hr.alg.iamu_project_bp.domain.model.City
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

interface CitySearch {
    data class Match(
        val city: City,
        val country: String?,
    )

    suspend fun searchCities(query: String): List<Match>
}

class OpenMeteoCitySearch(
    private val api: GeocodingApi,
) : CitySearch {
    override suspend fun searchCities(query: String): List<CitySearch.Match> {
        val trimmed = query.trim()

        if (trimmed.isEmpty()) return emptyList()

        return withContext(Dispatchers.IO) {
            val response = try {
                api.searchCities(name = trimmed)
            } catch (e: HttpException) {
                throw IOException("City search failed with HTTP ${e.code()}", e)
            }

            response.results
                ?.mapNotNull { it.toMatchOrNull() }
                .orEmpty()
        }
    }

    private fun ResultDto.toMatchOrNull(): CitySearch.Match? {
        val cityName = name ?: return null
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return CitySearch.Match(
            city = City(
                id = City.NEW_ID,
                name = cityName,
                latitude = lat,
                longitude = lon,
                isFavorite = false,
            ),
            country = country,
        )
    }
}
