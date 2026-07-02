package hr.alg.iamu_project_bp

import android.content.Context
import hr.alg.iamu_project_bp.domain.repository.CityRepository
import hr.alg.iamu_project_bp.domain.repository.WeatherDataSource
import hr.alg.iamu_project_bp.domain.repository.WeatherRepository

object AppGraph {
    @Volatile
    var cityRepositoryProvider: ((Context) -> CityRepository)? = null

    @Volatile
    var weatherRepositoryProvider: ((Context) -> WeatherRepository)? = null

    @Volatile
    var weatherDataSourceProvider: (() -> WeatherDataSource)? = null

    fun cityRepository(context: Context): CityRepository =
        requireNotNull(cityRepositoryProvider) { "AppGraph not initialized" }
            .invoke(context.applicationContext)

    fun weatherRepository(context: Context): WeatherRepository =
        requireNotNull(weatherRepositoryProvider) { "AppGraph not initialized" }
            .invoke(context.applicationContext)

    fun weatherDataSource(): WeatherDataSource =
        requireNotNull(weatherDataSourceProvider) { "AppGraph not initialized" }
            .invoke()
}
