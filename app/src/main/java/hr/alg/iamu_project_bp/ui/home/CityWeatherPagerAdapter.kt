package hr.alg.iamu_project_bp.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CityWeatherPagerAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment) {
    private val cityIds = mutableListOf<Long>()

    fun submit(ids: List<Long>) {
        cityIds.clear()
        cityIds.addAll(ids)
        notifyDataSetChanged()
    }

    fun cityIdAt(position: Int): Long? = cityIds.getOrNull(position)

    fun positionOf(cityId: Long): Int = cityIds.indexOf(cityId)

    override fun getItemCount(): Int = cityIds.size

    override fun getItemId(position: Int): Long = cityIds[position]

    override fun containsItem(itemId: Long): Boolean = cityIds.contains(itemId)

    override fun createFragment(position: Int): Fragment =
        CityWeatherPageFragment.newInstance(cityIds[position])
}
