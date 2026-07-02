package hr.alg.iamu_project_bp.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import hr.alg.iamu_project_bp.R
import hr.alg.iamu_project_bp.ui.anim.fadeInUp

class AboutFragment : Fragment(R.layout.fragment_about) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.txtAboutVersion).text =
            getString(R.string.about_version, appVersionName())

        view.findViewById<MaterialButton>(R.id.btnAboutOpenSource).setOnClickListener {
            openDataSource()
        }

        view.fadeInUp()
    }

    private fun appVersionName(): String = try {
        val ctx = requireContext()
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }

    private fun openDataSource() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_open_meteo)))

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(requireView(), R.string.browser_not_available, Snackbar.LENGTH_SHORT)
                .show()
        }
    }
}
