package hr.alg.iamu_project_bp.ui.home

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import hr.alg.iamu_project_bp.AppGraph
import hr.alg.iamu_project_bp.MainActivity
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.domain.repository.CityRepository
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var pager: ViewPager2
    private lateinit var emptyView: View
    private lateinit var adapter: CityWeatherPagerAdapter

    private var selectedCityId: Long = NO_CITY

    private val citiesChangedListener = CityRepository.OnCitiesChangedListener { loadCities() }
    private var registeredRepository: CityRepository? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pager = view.findViewById(R.id.weatherPager)
        emptyView = view.findViewById(R.id.groupHomeEmpty)
        view.findViewById<View>(R.id.btnAddCity).setOnClickListener {
            (activity as? MainActivity)?.openCities()
        }
        adapter = CityWeatherPagerAdapter(this)
        pager.adapter = adapter

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.cityIdAt(position)?.let { selectedCityId = it }
            }
        })

        savedInstanceState?.let { selectedCityId = it.getLong(STATE_SELECTED_CITY, NO_CITY) }

        setupMenu()
        registerCitiesListener()
        loadCities()
    }

    private fun setupMenu() {
        val host = requireActivity() as MenuHost
        host.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.home_menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
                R.id.action_share -> {
                    shareSelectedCity()
                    true
                }
                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun registerCitiesListener() {
        try {
            val repo = AppGraph.cityRepository(requireContext())
            repo.addOnCitiesChangedListener(citiesChangedListener)
            registeredRepository = repo
        } catch (e: IllegalArgumentException) {
        } catch (e: IllegalStateException) {
        }
    }

    private fun loadCities() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cities = try {
                AppGraph.cityRepository(requireContext()).getAll()
            } catch (e: IllegalArgumentException) {
                emptyList()
            } catch (e: IllegalStateException) {
                emptyList()
            }
            if (view == null) return@launch

            adapter.submit(cities.map { it.id })
            val hasCities = cities.isNotEmpty()
            emptyView.visibility = if (hasCities) View.GONE else View.VISIBLE
            pager.visibility = if (hasCities) View.VISIBLE else View.GONE

            if (hasCities) {
                val target = adapter.positionOf(selectedCityId).takeIf { it >= 0 } ?: 0
                pager.setCurrentItem(target, false)
                adapter.cityIdAt(target)?.let { selectedCityId = it }
            }
        }
    }

    private fun shareSelectedCity() {
        val position = pager.currentItem
        val cityId = adapter.cityIdAt(position) ?: return

        val page = childFragmentManager.findFragmentByTag("f$cityId")
        (page as? CityWeatherPageFragment)?.shareWeather()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_SELECTED_CITY, selectedCityId)
    }

    override fun onDestroyView() {
        registeredRepository?.removeOnCitiesChangedListener(citiesChangedListener)
        registeredRepository = null
        super.onDestroyView()
    }

    private companion object {
        const val NO_CITY = -1L
        const val STATE_SELECTED_CITY = "home_selected_city"
    }
}
