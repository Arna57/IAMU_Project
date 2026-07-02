package hr.alg.iamu_project_bp.ui.forecast

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hr.alg.iamu_project_bp.AppGraph
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.model.WeatherBundle
import kotlinx.coroutines.launch

class ForecastFragment : Fragment(R.layout.fragment_forecast) {
    private lateinit var list: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var cityHeader: TextView
    private val adapter = ForecastAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cityHeader = view.findViewById(R.id.txtForecastCity)
        emptyView = view.findViewById(R.id.txtForecastEmpty)
        list = view.findViewById(R.id.forecastList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        loadForecast()
    }

    override fun onResume() {
        super.onResume()

        loadForecast()
    }

    private fun loadForecast() {
        viewLifecycleOwner.lifecycleScope.launch {
            val bundle = try {
                resolveHomeBundle()
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IllegalStateException) {
                null
            }

            if (view == null) return@launch
            render(bundle)
        }
    }

    private suspend fun resolveHomeBundle(): WeatherBundle? {
        val cityRepo = AppGraph.cityRepository(requireContext())
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val prefKey = getString(R.string.pref_key_home_city_id)
        val homeId = prefs.getLong(prefKey, NO_CITY)

        val cityId = when {
            homeId != NO_CITY && cityRepo.getById(homeId) != null -> homeId
            else -> cityRepo.getAll().firstOrNull()?.id ?: return null
        }
        return AppGraph.weatherRepository(requireContext()).getCached(cityId)
    }

    private fun render(bundle: WeatherBundle?) {
        val days = bundle?.daily.orEmpty()
        if (days.isEmpty()) {
            cityHeader.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            adapter.submitList(emptyList())
            return
        }
        cityHeader.visibility = View.VISIBLE
        cityHeader.text = bundle?.city?.name
        emptyView.visibility = View.GONE
        adapter.submitList(days)
    }

    private companion object {
        const val NO_CITY = -1L
    }
}
