package hr.alg.iamu_project_bp.ui.cities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.model.City
import hr.alg.iamu_project_bp.ui.anim.staggerIn

class CityListAdapter(
    private val onFavorite: (City) -> Unit,
    private val onLongPress: (City) -> Unit,
) : ListAdapter<City, CityListAdapter.CityViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.staggerIn(position)
    }

    inner class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.txtCityName)
        private val coords: TextView = itemView.findViewById(R.id.txtCityCoords)
        private val favorite: ImageButton = itemView.findViewById(R.id.btnFavorite)

        fun bind(city: City) {
            name.text = city.name
            coords.text = itemView.context.getString(
                R.string.format_coords, city.latitude, city.longitude,
            )
            favorite.setImageResource(
                if (city.isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off,
            )
            favorite.setOnClickListener { onFavorite(city) }
            itemView.setOnLongClickListener {
                onLongPress(city)
                true
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<City>() {
            override fun areItemsTheSame(oldItem: City, newItem: City): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: City, newItem: City): Boolean =
                oldItem == newItem
        }
    }
}
