package hr.alg.iamu_project_bp.ui.forecast

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.WeatherCodes
import hr.alg.iamu_project_bp.domain.model.DailyForecast
import hr.alg.iamu_project_bp.ui.UnitsFormatter
import hr.alg.iamu_project_bp.ui.anim.staggerIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ForecastAdapter : ListAdapter<DailyForecast, ForecastAdapter.ForecastViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(getItem(position))

        holder.itemView.staggerIn(position)
    }

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

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<DailyForecast>() {
            override fun areItemsTheSame(oldItem: DailyForecast, newItem: DailyForecast): Boolean =
                oldItem.dateIso == newItem.dateIso

            override fun areContentsTheSame(oldItem: DailyForecast, newItem: DailyForecast): Boolean =
                oldItem == newItem
        }
    }
}
