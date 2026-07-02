package hr.alg.iamu_project_bp.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import hr.alg.iamu_project_bp.data.db.WeatherDbHelper
import hr.alg.iamu_project_bp.data.provider.CityProvider
import hr.alg.iamu_project_bp.domain.model.City
import hr.alg.iamu_project_bp.domain.repository.CityRepository
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteCityRepository private constructor(
    private val appContext: Context,
    private val dbHelper: WeatherDbHelper,
) : CityRepository {
    private val listeners = CopyOnWriteArraySet<CityRepository.OnCitiesChangedListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override suspend fun insert(city: City): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.writableDatabase.insertOrThrow(
            WeatherDbHelper.TABLE_CITIES,
            null,
            city.toContentValues(),
        )
        notifyCitiesChanged()
        id
    }

    override suspend fun getAll(): List<City> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            WeatherDbHelper.TABLE_CITIES,
            CityProvider.Contract.ALL_COLUMNS,
            null,
            null,
            null,
            null,
            "${WeatherDbHelper.COLUMN_CITY_NAME} COLLATE NOCASE ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toCity())
            }
        }
    }

    override suspend fun getById(id: Long): City? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            WeatherDbHelper.TABLE_CITIES,
            CityProvider.Contract.ALL_COLUMNS,
            "${WeatherDbHelper.COLUMN_CITY_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toCity() else null
        }
    }

    override suspend fun update(city: City): Boolean = withContext(Dispatchers.IO) {
        val rows = dbHelper.writableDatabase.update(
            WeatherDbHelper.TABLE_CITIES,
            city.toContentValues(),
            "${WeatherDbHelper.COLUMN_CITY_ID} = ?",
            arrayOf(city.id.toString()),
        )
        if (rows > 0) notifyCitiesChanged()
        rows > 0
    }

    override suspend fun delete(id: Long): Boolean = withContext(Dispatchers.IO) {
        val rows = dbHelper.writableDatabase.delete(
            WeatherDbHelper.TABLE_CITIES,
            "${WeatherDbHelper.COLUMN_CITY_ID} = ?",
            arrayOf(id.toString()),
        )
        if (rows > 0) notifyCitiesChanged()
        rows > 0
    }

    override fun addOnCitiesChangedListener(listener: CityRepository.OnCitiesChangedListener) {
        listeners += listener
    }

    override fun removeOnCitiesChangedListener(listener: CityRepository.OnCitiesChangedListener) {
        listeners -= listener
    }

    private fun notifyCitiesChanged() {
        mainHandler.post {
            appContext.contentResolver.notifyChange(CityProvider.Contract.CONTENT_URI, null)
            listeners.forEach { it.onCitiesChanged() }
        }
    }

    private fun City.toContentValues() = ContentValues().apply {
        if (id != City.NEW_ID) put(WeatherDbHelper.COLUMN_CITY_ID, id)
        put(WeatherDbHelper.COLUMN_CITY_NAME, name)
        put(WeatherDbHelper.COLUMN_CITY_LATITUDE, latitude)
        put(WeatherDbHelper.COLUMN_CITY_LONGITUDE, longitude)
        put(WeatherDbHelper.COLUMN_CITY_IS_FAVORITE, if (isFavorite) 1 else 0)
    }

    private fun Cursor.toCity() = City(
        id = getLong(getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_ID)),
        name = getString(getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_NAME)),
        latitude = getDouble(getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_LATITUDE)),
        longitude = getDouble(getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_LONGITUDE)),
        isFavorite = getInt(getColumnIndexOrThrow(WeatherDbHelper.COLUMN_CITY_IS_FAVORITE)) != 0,
    )

    companion object {
        @Volatile
        private var instance: SqliteCityRepository? = null

        fun getInstance(context: Context): SqliteCityRepository =
            instance ?: synchronized(this) {
                instance ?: SqliteCityRepository(
                    context.applicationContext,
                    WeatherDbHelper.getInstance(context),
                ).also { instance = it }
            }
    }
}
