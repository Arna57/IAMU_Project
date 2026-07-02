package hr.alg.iamu_project_bp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnticipateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.animation.ObjectAnimator
import android.view.View
import hr.alg.iamu_project_bp.MainActivity
import hr.alg.iamu_project_bp.ui.anim.Animations

class SplashActivity : AppCompatActivity() {
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !ready }
        window.decorView.postDelayed({ ready = true }, HOLD_MS)

        splashScreen.setOnExitAnimationListener { provider ->
            val icon: View = provider.iconView
            val slide = ObjectAnimator.ofFloat(
                icon, View.TRANSLATION_Y, 0f, -icon.height.toFloat(),
            )
            val fade = ObjectAnimator.ofFloat(icon, View.ALPHA, 1f, 0f)
            val zoom = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.4f)
            val zoomY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.4f)
            android.animation.AnimatorSet().apply {
                interpolator = AnticipateInterpolator()
                duration = Animations.DURATION_LONG
                playTogether(slide, fade, zoom, zoomY)
                doOnEnd {
                    provider.remove()
                    goToMain()
                }
                start()
            }
        }
    }

    private fun goToMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_FROM_SPLASH, true),
        )
        finish()
    }

    private companion object {
        const val HOLD_MS = 700L
    }
}
