package hr.alg.iamu_project_bp.ui.forecast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.model.DailyForecast
import hr.alg.iamu_project_bp.ui.anim.staggerIn

class ForecastAdapter : ListAdapter<DailyForecast, ForecastViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(getItem(position))

        holder.itemView.staggerIn(position)
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