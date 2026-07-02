package hr.alg.iamu_project_bp.ui.forecast

import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.WeatherCodes
import hr.alg.iamu_project_bp.domain.model.DailyForecast
import hr.alg.iamu_project_bp.ui.UnitsFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ForecastViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val icon: ImageView = itemView.findViewById(R.id.imgForecastIcon)
    private val date: TextView = itemView.findViewById(R.id.txtForecastDate)
    private val condition: TextView = itemView.findViewById(R.id.txtForecastCondition)
    private val tempRange: TextView = itemView.findViewById(R.id.txtForecastTempRange)
    private val precipitation: TextView = itemView.findViewById(R.id.txtForecastPrecipitation)
    private val wind: TextView = itemView.findViewById(R.id.txtForecastWind)

    fun bind(item: DailyForecast) {
        val res = itemView.resources
        icon.setImageResource(WeatherCodes.iconRes(item.weatherCode))
        date.text = formatDate(item.dateIso)
        condition.setText(WeatherCodes.descriptionRes(item.weatherCode))
        tempRange.text =
            UnitsFormatter.temperatureRange(itemView.context, item.minTempC, item.maxTempC)
        precipitation.text =
            res.getString(R.string.format_precipitation, item.precipitationMm)
        wind.text = UnitsFormatter.wind(itemView.context, item.windSpeedKmh)
    }

    private fun formatDate(dateIso: String): String = try {
        LocalDate.parse(dateIso).format(DATE_FORMAT)
    } catch (e: Exception) {
        dateIso
    }

    private companion object {
        val DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}
