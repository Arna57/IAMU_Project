package hr.alg.iamu_project_bp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import hr.alg.iamu_project_bp.databinding.ActivityMainBinding
import hr.alg.iamu_project_bp.ui.about.AboutFragment
import hr.alg.iamu_project_bp.ui.cities.CitiesFragment
import hr.alg.iamu_project_bp.ui.forecast.ForecastFragment
import hr.alg.iamu_project_bp.ui.home.HomeFragment
import hr.alg.iamu_project_bp.ui.settings.SettingsActivity
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.cd_drawer_open,
            R.string.cd_drawer_close,
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.nav_home -> {
                    navigateTo(HomeFragment(), addToBackStack = false)
                    true
                }
                R.id.nav_forecast -> {
                    navigateTo(ForecastFragment(), addToBackStack = true)
                    true
                }
                R.id.nav_cities -> {
                    navigateTo(CitiesFragment(), addToBackStack = true)
                    true
                }
                R.id.nav_about -> {
                    navigateTo(AboutFragment(), addToBackStack = true)
                    true
                }
                else -> false
            }
            if (handled) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            handled
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, HomeFragment())
            }
            maybeRequestNotificationPermission()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> {
            refreshHomeCity()
            true
        }
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.action_about -> {
            navigateTo(AboutFragment(), addToBackStack = true)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    fun openCities() {
        navigateTo(CitiesFragment(), addToBackStack = true)
    }

    private fun navigateTo(fragment: Fragment, addToBackStack: Boolean) {
        if (!addToBackStack) {
            supportFragmentManager.popBackStack(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
            )
        }
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.frag_slide_in_right,
                R.anim.frag_slide_out_left,
                R.anim.frag_slide_in_left,
                R.anim.frag_slide_out_right,
            )
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, fragment)
            if (addToBackStack) {
                addToBackStack(fragment::class.java.simpleName)
            }
        }
    }

    private fun refreshHomeCity() {
        if (!isOnline()) {
            Snackbar.make(binding.root, R.string.error_network, Snackbar.LENGTH_LONG).show()
            return
        }

        val progress = Snackbar.make(binding.root, R.string.message_refreshing, Snackbar.LENGTH_INDEFINITE)
        progress.show()
        lifecycleScope.launch {
            try {
                val cityId = resolveHomeCityId()
                if (cityId == null) {
                    progress.dismiss()
                    Snackbar.make(binding.root, R.string.error_no_data, Snackbar.LENGTH_SHORT).show()
                    return@launch
                }
                AppGraph.weatherRepository(this@MainActivity).refresh(cityId)
                progress.dismiss()
                Snackbar.make(binding.root, R.string.message_refresh_success, Snackbar.LENGTH_SHORT)
                    .show()
            } catch (e: IOException) {
                progress.dismiss()
                Snackbar.make(binding.root, R.string.error_network, Snackbar.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                progress.dismiss()
                Snackbar.make(binding.root, R.string.message_refresh_failed, Snackbar.LENGTH_SHORT)
                    .show()
            } catch (e: IllegalStateException) {
                progress.dismiss()
                Snackbar.make(binding.root, R.string.message_refresh_failed, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private suspend fun resolveHomeCityId(): Long? {
        val cityRepo = AppGraph.cityRepository(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val homeId = prefs.getLong(getString(R.string.pref_key_home_city_id), -1L)
        if (homeId != -1L && cityRepo.getById(homeId) != null) return homeId
        return cityRepo.getAll().firstOrNull()?.id
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean(KEY_PERMISSION_ASKED, false)) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()

        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_rationale_title)
                .setMessage(R.string.notification_permission_rationale)
                .setNegativeButton(R.string.action_not_now, null)
                .setPositiveButton(R.string.action_allow) { _, _ ->
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .show()
        } else {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_FROM_SPLASH = "hr.alg.iamu_project_bp.extra.FROM_SPLASH"

        private const val KEY_PERMISSION_ASKED = "ui_notification_permission_asked"
    }
}
