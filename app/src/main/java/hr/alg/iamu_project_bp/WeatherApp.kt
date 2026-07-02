package hr.alg.iamu_project_bp

import android.app.Application
import hr.alg.iamu_project_bp.data.network.NetworkModule
import hr.alg.iamu_project_bp.data.repository.SqliteCityRepository
import hr.alg.iamu_project_bp.data.repository.SqliteWeatherRepository
import hr.alg.iamu_project_bp.work.SyncScheduler

class WeatherApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AppGraph.cityRepositoryProvider = { SqliteCityRepository.getInstance(it) }
        AppGraph.weatherDataSourceProvider = { NetworkModule.weatherDataSource() }
        AppGraph.weatherRepositoryProvider = {
            SqliteWeatherRepository.getInstance(it, NetworkModule.weatherDataSource())
        }

        SyncScheduler.schedulePeriodic(this)
    }
}
