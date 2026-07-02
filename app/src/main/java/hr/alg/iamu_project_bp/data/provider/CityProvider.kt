package hr.alg.iamu_project_bp.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import hr.alg.iamu_project_bp.data.db.WeatherDbHelper

class CityProvider : ContentProvider() {
    object Contract {
        const val AUTHORITY = "hr.alg.iamu_project_bp.provider"
        const val PATH_CITIES = "cities"

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_CITIES")

        const val CONTENT_TYPE_DIR =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_CITIES"

        const val CONTENT_TYPE_ITEM =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_CITIES"

        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_IS_FAVORITE = "is_favorite"

        val ALL_COLUMNS = arrayOf(
            COLUMN_ID, COLUMN_NAME, COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_IS_FAVORITE
        )
    }

    private lateinit var dbHelper: WeatherDbHelper

    override fun onCreate(): Boolean {
        dbHelper = WeatherDbHelper.getInstance(requireNotNull(context))
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = dbHelper.readableDatabase.query(
            WeatherDbHelper.TABLE_CITIES,
            projection ?: Contract.ALL_COLUMNS,
            selectionFor(uri, selection),
            selectionArgsFor(uri, selectionArgs),
            null,
            null,
            sortOrder ?: "${Contract.COLUMN_NAME} COLLATE NOCASE ASC",
        )

        cursor.setNotificationUri(requireNotNull(context).contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String = when (uriMatcher.match(uri)) {
        MATCH_CITIES_DIR -> Contract.CONTENT_TYPE_DIR
        MATCH_CITY_ITEM -> Contract.CONTENT_TYPE_ITEM
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        require(uriMatcher.match(uri) == MATCH_CITIES_DIR) { "Insert not supported on $uri" }
        val id = dbHelper.writableDatabase.insertOrThrow(
            WeatherDbHelper.TABLE_CITIES,
            null,
            values,
        )
        val insertedUri = ContentUris.withAppendedId(Contract.CONTENT_URI, id)
        notifyChange()
        return insertedUri
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val rows = dbHelper.writableDatabase.update(
            WeatherDbHelper.TABLE_CITIES,
            values,
            selectionFor(uri, selection),
            selectionArgsFor(uri, selectionArgs),
        )
        if (rows > 0) notifyChange()
        return rows
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val rows = dbHelper.writableDatabase.delete(
            WeatherDbHelper.TABLE_CITIES,
            selectionFor(uri, selection),
            selectionArgsFor(uri, selectionArgs),
        )
        if (rows > 0) notifyChange()
        return rows
    }

    private fun selectionFor(uri: Uri, selection: String?): String? =
        when (uriMatcher.match(uri)) {
            MATCH_CITIES_DIR -> selection
            MATCH_CITY_ITEM ->
                if (selection.isNullOrEmpty()) {
                    "${Contract.COLUMN_ID} = ?"
                } else {
                    "${Contract.COLUMN_ID} = ? AND ($selection)"
                }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

    private fun selectionArgsFor(
        uri: Uri,
        selectionArgs: Array<out String>?,
    ): Array<out String>? = when (uriMatcher.match(uri)) {
        MATCH_CITIES_DIR -> selectionArgs
        MATCH_CITY_ITEM ->
            arrayOf(ContentUris.parseId(uri).toString(), *(selectionArgs ?: emptyArray()))
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    private fun notifyChange() {
        requireNotNull(context).contentResolver.notifyChange(Contract.CONTENT_URI, null)
    }

    private companion object {
        const val MATCH_CITIES_DIR = 1
        const val MATCH_CITY_ITEM = 2

        val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(Contract.AUTHORITY, Contract.PATH_CITIES, MATCH_CITIES_DIR)
            addURI(Contract.AUTHORITY, "${Contract.PATH_CITIES}/#", MATCH_CITY_ITEM)
        }
    }
}
