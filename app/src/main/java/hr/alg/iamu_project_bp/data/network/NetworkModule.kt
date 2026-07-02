package hr.alg.iamu_project_bp.data.network

import com.google.gson.Gson
import hr.alg.iamu_project_bp.domain.repository.WeatherDataSource
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object NetworkModule {
    private val gson: Gson by lazy { Gson() }

    private val forecastApi: ForecastApi by lazy {
        retrofit(ForecastApi.BASE_URL).create()
    }

    private val geocodingApi: GeocodingApi by lazy {
        retrofit(GeocodingApi.BASE_URL).create()
    }

    private val weatherDataSource: WeatherDataSource by lazy {
        OpenMeteoWeatherDataSource(forecastApi)
    }

    private val citySearch: CitySearch by lazy {
        OpenMeteoCitySearch(geocodingApi)
    }

    fun weatherDataSource(): WeatherDataSource = weatherDataSource

    fun citySearch(): CitySearch = citySearch

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
}
