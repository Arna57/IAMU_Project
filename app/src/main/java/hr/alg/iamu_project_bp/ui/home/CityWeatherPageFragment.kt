package hr.alg.iamu_project_bp.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import hr.alg.iamu_project_bp.AppGraph
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.WeatherCodes
import hr.alg.iamu_project_bp.domain.model.WeatherBundle
import hr.alg.iamu_project_bp.ui.UnitsFormatter
import hr.alg.iamu_project_bp.ui.anim.fadeInUp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class CityWeatherPageFragment : Fragment(R.layout.page_city_weather) {
    private val cityId: Long
        get() = requireArguments().getLong(ARG_CITY_ID)

    private var bundle: WeatherBundle? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btnShare).setOnClickListener { shareWeather() }
        view.findViewById<MaterialButton>(R.id.btnOpenInMaps).setOnClickListener { openInMaps() }

        loadWeather()
    }

    override fun onResume() {
        super.onResume()

        view?.let { render(it, bundle) }
    }

    private fun loadWeather() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = try {
                AppGraph.weatherRepository(requireContext()).getCached(cityId)
            } catch (e: IllegalArgumentException) {
                null
            } catch (e: IllegalStateException) {
                null
            }
            bundle = result
            val root = view ?: return@launch
            render(root, result)
        }
    }

    private fun render(root: View, data: WeatherBundle?) {
        val name = root.findViewById<TextView>(R.id.txtCityName)
        val icon = root.findViewById<ImageView>(R.id.imgCondition)
        val temp = root.findViewById<TextView>(R.id.txtTemperature)
        val condition = root.findViewById<TextView>(R.id.txtCondition)
        val wind = root.findViewById<TextView>(R.id.txtWind)
        val humidity = root.findViewById<TextView>(R.id.txtHumidity)
        val updated = root.findViewById<TextView>(R.id.txtUpdatedAt)
        val empty = root.findViewById<TextView>(R.id.txtPageEmpty)

        if (data == null) {
            empty.visibility = View.VISIBLE
            temp.text = ""
            condition.text = ""
            wind.text = ""
            humidity.text = ""
            updated.text = ""
            return
        }

        empty.visibility = View.GONE
        val current = data.current
        name.text = data.city.name
        icon.setImageResource(WeatherCodes.iconRes(current.weatherCode))
        temp.text = UnitsFormatter.temperature(requireContext(), current.temperatureC)
        condition.setText(WeatherCodes.descriptionRes(current.weatherCode))
        wind.text = getString(R.string.label_wind) + ": " +
            UnitsFormatter.wind(requireContext(), current.windSpeedKmh)
        humidity.text = getString(R.string.label_humidity) + ": " +
            getString(R.string.format_humidity, current.humidityPercent)
        val time = DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(Date(current.updatedAtEpochMs))
        updated.text = getString(R.string.label_updated_at, time)

        icon.fadeInUp()
        temp.fadeInUp(startDelay = 40L)
        condition.fadeInUp(startDelay = 80L)
    }

    fun shareWeather() {
        val data = bundle
        val cityName = data?.city?.name ?: getString(R.string.title_home)
        val body = if (data != null) {
            getString(
                R.string.share_weather_body,
                cityName,
                UnitsFormatter.temperature(requireContext(), data.current.temperatureC),
                getString(WeatherCodes.descriptionRes(data.current.weatherCode)),
            )
        } else {
            cityName
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_weather_subject, cityName))
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
    }

    private fun openInMaps() {
        val data = bundle
        val city = data?.city
        val lat = city?.latitude
        val lon = city?.longitude
        if (lat == null || lon == null) {
            Snackbar.make(requireView(), R.string.error_no_data, Snackbar.LENGTH_SHORT).show()
            return
        }
        val label = Uri.encode(city.name)
        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
        val view = Intent(Intent.ACTION_VIEW, geoUri).apply {
            putExtra(EXTRA_CITY_NAME, city.name)
        }

        try {
            startActivity(view)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(requireView(), R.string.maps_not_available, Snackbar.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_CITY_ID = "city_id"
        private const val EXTRA_CITY_NAME = "hr.alg.iamu_project_bp.extra.CITY_NAME"

        fun newInstance(cityId: Long): CityWeatherPageFragment =
            CityWeatherPageFragment().apply {
                arguments = Bundle().apply { putLong(ARG_CITY_ID, cityId) }
            }
    }
}
