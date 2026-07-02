package hr.alg.iamu_project_bp.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import hr.alg.iamu_project_bp.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
