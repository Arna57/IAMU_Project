package hr.alg.iamu_project_bp.data.network

import hr.alg.iamu_project_bp.data.network.dto.ForecastResponseDto
import hr.alg.iamu_project_bp.domain.model.CurrentWeather
import hr.alg.iamu_project_bp.domain.model.DailyForecast
import hr.alg.iamu_project_bp.domain.model.WeatherSnapshot

object WeatherResponseParser {
    fun toSnapshot(dto: ForecastResponseDto): WeatherSnapshot =
        WeatherSnapshot(
            current = parseCurrent(dto.current),
            daily = parseDaily(dto.daily),
        )

    private fun parseCurrent(current: ForecastResponseDto.CurrentDto?): CurrentWeather {
        checkNotNull(current) { "Malformed forecast payload: missing 'current'" }
        return CurrentWeather(
            temperatureC = checkNotNull(current.temperature2m) {
                "Malformed forecast payload: missing current.temperature_2m"
            },
            weatherCode = checkNotNull(current.weatherCode) {
                "Malformed forecast payload: missing current.weather_code"
            },
            windSpeedKmh = checkNotNull(current.windSpeed10m) {
                "Malformed forecast payload: missing current.wind_speed_10m"
            },
            humidityPercent = checkNotNull(current.relativeHumidity2m) {
                "Malformed forecast payload: missing current.relative_humidity_2m"
            },
            updatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun parseDaily(daily: ForecastResponseDto.DailyDto?): List<DailyForecast> {
        checkNotNull(daily) { "Malformed forecast payload: missing 'daily'" }
        val dates = checkNotNull(daily.time) {
            "Malformed forecast payload: missing daily.time"
        }
        val codes = checkNotNull(daily.weatherCode) {
            "Malformed forecast payload: missing daily.weather_code"
        }
        val mins = checkNotNull(daily.temperatureMin) {
            "Malformed forecast payload: missing daily.temperature_2m_min"
        }
        val maxs = checkNotNull(daily.temperatureMax) {
            "Malformed forecast payload: missing daily.temperature_2m_max"
        }
        val precip = checkNotNull(daily.precipitationSum) {
            "Malformed forecast payload: missing daily.precipitation_sum"
        }
        val winds = checkNotNull(daily.windSpeedMax) {
            "Malformed forecast payload: missing daily.wind_speed_10m_max"
        }

        val size = dates.size
        check(
            codes.size == size && mins.size == size && maxs.size == size &&
                precip.size == size && winds.size == size,
        ) { "Malformed forecast payload: daily arrays have mismatched lengths" }

        return List(size) { i ->
            DailyForecast(
                dateIso = dates[i],
                minTempC = mins[i],
                maxTempC = maxs[i],
                weatherCode = codes[i],
                precipitationMm = precip[i],
                windSpeedKmh = winds[i],
            )
        }
    }
}
