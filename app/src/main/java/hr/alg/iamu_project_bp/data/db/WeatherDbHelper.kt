package hr.alg.iamu_project_bp.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import hr.alg.iamu_project_bp.data.provider.CityProvider

class WeatherDbHelper private constructor(
    context: Context,
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)

        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_CITIES)
        db.execSQL(SQL_CREATE_CURRENT_WEATHER)
        db.execSQL(SQL_CREATE_DAILY_FORECAST)
        db.execSQL(SQL_CREATE_DAILY_FORECAST_INDEX)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DAILY_FORECAST")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CURRENT_WEATHER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CITIES")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        const val DATABASE_NAME = "weather.db"
        const val DATABASE_VERSION = 1

        const val TABLE_CITIES = "cities"
        const val COLUMN_CITY_ID = CityProvider.Contract.COLUMN_ID
        const val COLUMN_CITY_NAME = CityProvider.Contract.COLUMN_NAME
        const val COLUMN_CITY_LATITUDE = CityProvider.Contract.COLUMN_LATITUDE
        const val COLUMN_CITY_LONGITUDE = CityProvider.Contract.COLUMN_LONGITUDE
        const val COLUMN_CITY_IS_FAVORITE = CityProvider.Contract.COLUMN_IS_FAVORITE

        const val TABLE_CURRENT_WEATHER = "current_weather"
        const val COLUMN_WEATHER_ID = "_id"
        const val COLUMN_WEATHER_CITY_ID = "city_id"
        const val COLUMN_WEATHER_TEMPERATURE_C = "temperature_c"
        const val COLUMN_WEATHER_CODE = "weather_code"
        const val COLUMN_WEATHER_WIND_SPEED_KMH = "wind_speed_kmh"
        const val COLUMN_WEATHER_HUMIDITY_PERCENT = "humidity_percent"
        const val COLUMN_WEATHER_UPDATED_AT = "updated_at"

        const val TABLE_DAILY_FORECAST = "daily_forecast"
        const val COLUMN_FORECAST_ID = "_id"
        const val COLUMN_FORECAST_CITY_ID = "city_id"
        const val COLUMN_FORECAST_DATE_ISO = "date_iso"
        const val COLUMN_FORECAST_MIN_TEMP_C = "min_temp_c"
        const val COLUMN_FORECAST_MAX_TEMP_C = "max_temp_c"
        const val COLUMN_FORECAST_WEATHER_CODE = "weather_code"
        const val COLUMN_FORECAST_PRECIPITATION_MM = "precipitation_mm"
        const val COLUMN_FORECAST_WIND_SPEED_KMH = "wind_speed_kmh"

        private const val SQL_CREATE_CITIES = """
            CREATE TABLE $TABLE_CITIES (
                $COLUMN_CITY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CITY_NAME TEXT NOT NULL,
                $COLUMN_CITY_LATITUDE REAL NOT NULL,
                $COLUMN_CITY_LONGITUDE REAL NOT NULL,
                $COLUMN_CITY_IS_FAVORITE INTEGER NOT NULL DEFAULT 0
            )
        """

        private const val SQL_CREATE_CURRENT_WEATHER = """
            CREATE TABLE $TABLE_CURRENT_WEATHER (
                $COLUMN_WEATHER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WEATHER_CITY_ID INTEGER NOT NULL UNIQUE
                    REFERENCES $TABLE_CITIES($COLUMN_CITY_ID) ON DELETE CASCADE,
                $COLUMN_WEATHER_TEMPERATURE_C REAL NOT NULL,
                $COLUMN_WEATHER_CODE INTEGER NOT NULL,
                $COLUMN_WEATHER_WIND_SPEED_KMH REAL NOT NULL,
                $COLUMN_WEATHER_HUMIDITY_PERCENT INTEGER NOT NULL,
                $COLUMN_WEATHER_UPDATED_AT INTEGER NOT NULL
            )
        """

        private const val SQL_CREATE_DAILY_FORECAST = """
            CREATE TABLE $TABLE_DAILY_FORECAST (
                $COLUMN_FORECAST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FORECAST_CITY_ID INTEGER NOT NULL
                    REFERENCES $TABLE_CITIES($COLUMN_CITY_ID) ON DELETE CASCADE,
                $COLUMN_FORECAST_DATE_ISO TEXT NOT NULL,
                $COLUMN_FORECAST_MIN_TEMP_C REAL NOT NULL,
                $COLUMN_FORECAST_MAX_TEMP_C REAL NOT NULL,
                $COLUMN_FORECAST_WEATHER_CODE INTEGER NOT NULL,
                $COLUMN_FORECAST_PRECIPITATION_MM REAL NOT NULL,
                $COLUMN_FORECAST_WIND_SPEED_KMH REAL NOT NULL
            )
        """

        private const val SQL_CREATE_DAILY_FORECAST_INDEX =
            "CREATE INDEX index_${TABLE_DAILY_FORECAST}_city_id " +
                "ON $TABLE_DAILY_FORECAST($COLUMN_FORECAST_CITY_ID)"

        @Volatile
        private var instance: WeatherDbHelper? = null

        fun getInstance(context: Context): WeatherDbHelper =
            instance ?: synchronized(this) {
                instance ?: WeatherDbHelper(context.applicationContext).also { instance = it }
            }
    }
}
