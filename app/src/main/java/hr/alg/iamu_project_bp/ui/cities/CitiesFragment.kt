package hr.alg.iamu_project_bp.ui.cities

import android.content.ContentUris
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.data.network.CitySearch
import hr.alg.iamu_project_bp.data.network.ConnectivityChecker
import hr.alg.iamu_project_bp.data.network.NetworkModule
import hr.alg.iamu_project_bp.data.provider.CityProvider
import hr.alg.iamu_project_bp.domain.model.City
import hr.alg.iamu_project_bp.work.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class CitiesFragment : Fragment(R.layout.fragment_cities) {
    private lateinit var list: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: CityListAdapter

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = loadCities()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyView = view.findViewById(R.id.txtCitiesEmpty)
        list = view.findViewById(R.id.citiesList)
        adapter = CityListAdapter(onFavorite = ::toggleFavorite, onLongPress = ::showEditOrDeleteDialog)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fabAddCity).setOnClickListener {
            showAddDialog()
        }

        requireContext().contentResolver.registerContentObserver(
            CityProvider.Contract.CONTENT_URI, true, observer,
        )
        loadCities()
    }

    override fun onDestroyView() {
        requireContext().contentResolver.unregisterContentObserver(observer)
        super.onDestroyView()
    }

    private fun loadCities() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cities = withContext(Dispatchers.IO) { queryCities() }
            if (view == null) return@launch
            adapter.submitList(cities)
            emptyView.visibility = if (cities.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun queryCities(): List<City> {
        val cursor: Cursor = requireContext().contentResolver.query(
            CityProvider.Contract.CONTENT_URI,
            CityProvider.Contract.ALL_COLUMNS,
            null,
            null,
            CityProvider.Contract.COLUMN_NAME,
        ) ?: return emptyList()

        return cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(CityProvider.Contract.COLUMN_ID)
            val nameIdx = c.getColumnIndexOrThrow(CityProvider.Contract.COLUMN_NAME)
            val latIdx = c.getColumnIndexOrThrow(CityProvider.Contract.COLUMN_LATITUDE)
            val lonIdx = c.getColumnIndexOrThrow(CityProvider.Contract.COLUMN_LONGITUDE)
            val favIdx = c.getColumnIndexOrThrow(CityProvider.Contract.COLUMN_IS_FAVORITE)
            buildList {
                while (c.moveToNext()) {
                    add(
                        City(
                            id = c.getLong(idIdx),
                            name = c.getString(nameIdx),
                            latitude = c.getDouble(latIdx),
                            longitude = c.getDouble(lonIdx),
                            isFavorite = c.getInt(favIdx) != 0,
                        ),
                    )
                }
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_search_city, null)
        val queryInput = dialogView.findViewById<TextInputEditText>(R.id.inputSearchQuery)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_add_city_title)
            .setView(dialogView)
            .setNegativeButton(R.string.action_cancel, null)
            .setNeutralButton(R.string.action_manual_entry) { _, _ -> showEditor(existing = null) }
            .setPositiveButton(R.string.action_search) { _, _ ->
                searchAndPickCity(queryInput.text?.toString()?.trim().orEmpty())
            }
            .show()
    }

    private fun searchAndPickCity(query: String) {
        if (query.isEmpty()) {
            Snackbar.make(requireView(), R.string.error_invalid_input, Snackbar.LENGTH_SHORT).show()
            return
        }
        if (!ConnectivityChecker.isOnline(requireContext())) {
            Snackbar.make(requireView(), R.string.error_network, Snackbar.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val matches = try {
                NetworkModule.citySearch().searchCities(query)
            } catch (e: IOException) {
                Snackbar.make(requireView(), R.string.error_network, Snackbar.LENGTH_SHORT).show()
                return@launch
            }
            if (matches.isEmpty()) {
                Snackbar.make(
                    requireView(),
                    getString(R.string.search_no_results, query),
                    Snackbar.LENGTH_LONG,
                ).show()
                return@launch
            }
            showSearchResults(matches)
        }
    }

    private fun showSearchResults(matches: List<CitySearch.Match>) {
        val labels = matches.map { match ->
            val where = match.country
                ?: getString(R.string.format_coords, match.city.latitude, match.city.longitude)
            "${match.city.name} — $where"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_add_city_title)
            .setItems(labels) { _, which ->
                val city = matches[which].city
                saveCity(existing = null, name = city.name, lat = city.latitude, lon = city.longitude)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showEditOrDeleteDialog(city: City) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(city.name)
            .setItems(
                arrayOf(getString(R.string.action_edit), getString(R.string.action_delete)),
            ) { _, which ->
                when (which) {
                    0 -> showEditor(existing = city)
                    1 -> showDeleteDialog(city)
                }
            }
            .show()
    }

    private fun showEditor(existing: City?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_city, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.inputCityName)
        val latInput = dialogView.findViewById<TextInputEditText>(R.id.inputLatitude)
        val lonInput = dialogView.findViewById<TextInputEditText>(R.id.inputLongitude)

        existing?.let {
            nameInput.setText(it.name)
            latInput.setText(it.latitude.toString())
            lonInput.setText(it.longitude.toString())
        }

        val titleRes =
            if (existing == null) R.string.dialog_add_city_title else R.string.dialog_edit_city_title

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(dialogView)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = nameInput.text?.toString()?.trim().orEmpty()
                val lat = latInput.text?.toString()?.trim()?.toDoubleOrNull()
                val lon = lonInput.text?.toString()?.trim()?.toDoubleOrNull()
                if (name.isEmpty() || lat == null || lon == null) {
                    Snackbar.make(requireView(), R.string.error_invalid_input, Snackbar.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }
                saveCity(existing, name, lat, lon)
            }
            .show()
    }

    private fun saveCity(existing: City?, name: String, lat: Double, lon: Double) {
        val values = ContentValues().apply {
            put(CityProvider.Contract.COLUMN_NAME, name)
            put(CityProvider.Contract.COLUMN_LATITUDE, lat)
            put(CityProvider.Contract.COLUMN_LONGITUDE, lon)
            if (existing == null) {
                put(CityProvider.Contract.COLUMN_IS_FAVORITE, 0)
            }
        }
        val resolver = requireContext().contentResolver
        val cityId = if (existing == null) {
            resolver.insert(CityProvider.Contract.CONTENT_URI, values)
                ?.let(ContentUris::parseId)
        } else {
            resolver.update(itemUri(existing.id), values, null, null)
            existing.id
        }

        cityId?.let { SyncScheduler.triggerOneShotSync(requireContext(), it) }
        Snackbar.make(requireView(), R.string.message_city_saved, Snackbar.LENGTH_SHORT).show()
        loadCities()
    }

    private fun showDeleteDialog(city: City) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_city_title)
            .setMessage(getString(R.string.dialog_delete_city_message, city.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                requireContext().contentResolver.delete(itemUri(city.id), null, null)
                Snackbar.make(requireView(), R.string.message_city_deleted, Snackbar.LENGTH_SHORT)
                    .show()
                loadCities()
            }
            .show()
    }

    private fun toggleFavorite(city: City) {
        val newValue = !city.isFavorite
        val values = ContentValues().apply {
            put(CityProvider.Contract.COLUMN_IS_FAVORITE, if (newValue) 1 else 0)
        }
        requireContext().contentResolver.update(itemUri(city.id), values, null, null)

        if (newValue) {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit()
                .putLong(getString(R.string.pref_key_home_city_id), city.id)
                .apply()
            Snackbar.make(
                requireView(),
                getString(R.string.home_city_set, city.name),
                Snackbar.LENGTH_SHORT,
            ).show()
        }
        loadCities()
    }

    private fun itemUri(id: Long): Uri =
        ContentUris.withAppendedId(CityProvider.Contract.CONTENT_URI, id)
}
