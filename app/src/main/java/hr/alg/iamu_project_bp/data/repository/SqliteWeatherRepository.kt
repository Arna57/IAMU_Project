package hr.alg.iamu_project_bp.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import hr.alg.iamu_project_bp.data.db.WeatherDbHelper
import hr.alg.iamu_project_bp.domain.model.City
import hr.alg.iamu_project_bp.domain.model.CurrentWeather
import hr.alg.iamu_project_bp.domain.model.DailyForecast
import hr.alg.iamu_project_bp.domain.model.WeatherBundle
import hr.alg.iamu_project_bp.domain.model.WeatherSnapshot
import hr.alg.iamu_project_bp.domain.repository.WeatherDataSource
import hr.alg.iamu_project_bp.domain.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteWeatherRepository private constructor(
    private val dbHelper: WeatherDbHelper,
    private val dataSource: WeatherDataSource,
) : WeatherRepository {
    override suspend fun getCached(cityId: Long): WeatherBundle? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val city = db.queryCity(cityId) ?: return@withContext null
        val current = db.queryCurrentWeather(cityId) ?: return@withContext null
        WeatherBundle(city, current, db.queryDailyForecasts(cityId))
    }

    override suspend fun refresh(cityId: Long): WeatherBundle = withContext(Dispatchers.IO) {
        val city = dbHelper.readableDatabase.queryCity(cityId)
            ?: throw IllegalArgumentException("No city with id $cityId")

        val snapshot = dataSource.fetchWeather(city.latitude, city.longitude)

        persist(cityId, snapshot)
        WeatherBundle(city, snapshot.current, snapshot.daily)
    }

    private fun persist(cityId: Long, snapshot: WeatherSnapshot) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict(
                WeatherDbHelper.TABLE_CURRENT_WEATHER,
                null,
                snapshot.current.toContentValues(cityId),
                SQLiteDatabase.CONFLICT_REPLACE,
            )
            db.delete(
                WeatherDbHelper.TABLE_DAILY_FORECAST,
                "${WeatherDbHelper.COLUMN_FORECAST_CITY_ID} = ?",
                arrayOf(cityId.toString()),
            )
            snapshot.daily.forEach { day ->
                db.insertOrThrow(
                    WeatherDbHelper.TABLE_DAILY_FORECAST,
                    null,
                    day.toContentValues(cityId),
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun SQLiteDatabase.queryCity(cityId: Long): City? = query(
        WeatherDbHelper.TABLE_CITIES,
        null,
        "${WeatherDbHelper.COLUMN_CITY_ID} = ?",
        arrayOf(cityId.toString()),
        null,
        null,
        null,
    ).use { cursor ->
        if (!cursor.moveToFirst()) return null
        City(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_NAME)),
            latitude = cursor.getDouble(
                cursor.getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_LATITUDE)
            ),
            longitude = cursor.getDouble(
                cursor.getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_LONGITUDE)
            ),
            isFavorite = cursor.getInt(
                cursor.getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_IS_FAVORITE)
            ) != 0,
        )
    }

    private fun SQLiteDatabase.queryCurrentWeather(cityId: Long): CurrentWeather? = query(
        WeatherDbHelper.TABLE_CURRENT_WEATHER,
        null,
        "${WeatherDbHelper.COLUMN_WEATHER_CITY_ID} = ?",
        arrayOf(cityId.toString()),
        null,
        null,
        null,
    ).use { cursor ->
        if (!cursor.moveToFirst()) return null
        CurrentWeather(
            temperatureC = cursor.getDoubleBy(WeatherDbHelper.COLUMN_WEATHER_TEMPERATURE_C),
            weatherCode = cursor.getIntBy(WeatherDbHelper.COLUMN_WEATHER_CODE),
            windSpeedKmh = cursor.getDoubleBy(WeatherDbHelper.COLUMN_WEATHER_WIND_SPEED_KMH),
            humidityPercent = cursor.getIntBy(WeatherDbHelper.COLUMN_WEATHER_HUMIDITY_PERCENT),
            updatedAtEpochMs = cursor.getLongBy(WeatherDbHelper.COLUMN_WEATHER_UPDATED_AT),
        )
    }

    private fun SQLiteDatabase.queryDailyForecasts(cityId: Long): List<DailyForecast> = query(
        WeatherDbHelper.TABLE_DAILY_FORECAST,
        null,
        "${WeatherDbHelper.COLUMN_FORECAST_CITY_ID} = ?",
        arrayOf(cityId.toString()),
        null,
        null,
        "${WeatherDbHelper.COLUMN_FORECAST_DATE_ISO} ASC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    DailyForecast(
                        dateIso = cursor.getString(
                            cursor.getColumnIndexOrThrow(WeatherDbHelper.COLUMN_FORECAST_DATE_ISO)
                        ),
                        minTempC = cursor.getDoubleBy(WeatherDbHelper.COLUMN_FORECAST_MIN_TEMP_C),
                        maxTempC = cursor.getDoubleBy(WeatherDbHelper.COLUMN_FORECAST_MAX_TEMP_C),
                        weatherCode = cursor.getIntBy(WeatherDbHelper.COLUMN_FORECAST_WEATHER_CODE),
                        precipitationMm = cursor.getDoubleBy(
                            WeatherDbHelper.COLUMN_FORECAST_PRECIPITATION_MM
                        ),
                        windSpeedKmh = cursor.getDoubleBy(
                            WeatherDbHelper.COLUMN_FORECAST_WIND_SPEED_KMH
                        ),
                    )
                )
            }
        }
    }

    private fun Cursor.getIntBy(column: String) = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.getLongBy(column: String) = getLong(getColumnIndexOrThrow(column))
    private fun Cursor.getDoubleBy(column: String) = getDouble(getColumnIndexOrThrow(column))

    private fun CurrentWeather.toContentValues(cityId: Long) = ContentValues().apply {
        put(WeatherDbHelper.COLUMN_WEATHER_CITY_ID, cityId)
        put(WeatherDbHelper.COLUMN_WEATHER_TEMPERATURE_C, temperatureC)
        put(WeatherDbHelper.COLUMN_WEATHER_CODE, weatherCode)
        put(WeatherDbHelper.COLUMN_WEATHER_WIND_SPEED_KMH, windSpeedKmh)
        put(WeatherDbHelper.COLUMN_WEATHER_HUMIDITY_PERCENT, humidityPercent)
        put(WeatherDbHelper.COLUMN_WEATHER_UPDATED_AT, updatedAtEpochMs)
    }

    private fun DailyForecast.toContentValues(cityId: Long) = ContentValues().apply {
        put(WeatherDbHelper.COLUMN_FORECAST_CITY_ID, cityId)
        put(WeatherDbHelper.COLUMN_FORECAST_DATE_ISO, dateIso)
        put(WeatherDbHelper.COLUMN_FORECAST_MIN_TEMP_C, minTempC)
        put(WeatherDbHelper.COLUMN_FORECAST_MAX_TEMP_C, maxTempC)
        put(WeatherDbHelper.COLUMN_FORECAST_WEATHER_CODE, weatherCode)
        put(WeatherDbHelper.COLUMN_FORECAST_PRECIPITATION_MM, precipitationMm)
        put(WeatherDbHelper.COLUMN_FORECAST_WIND_SPEED_KMH, windSpeedKmh)
    }

    companion object {
        @Volatile
        private var instance: SqliteWeatherRepository? = null

        fun getInstance(context: Context, dataSource: WeatherDataSource): SqliteWeatherRepository =
            instance ?: synchronized(this) {
                instance ?: SqliteWeatherRepository(
                    WeatherDbHelper.getInstance(context),
                    dataSource,
                ).also { instance = it }
            }
    }
}
