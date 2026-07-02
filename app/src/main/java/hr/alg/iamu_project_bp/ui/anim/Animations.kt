package hr.alg.iamu_project_bp.ui.anim

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

object Animations {
    const val DURATION_SHORT = 180L
    const val DURATION_MEDIUM = 300L
    const val DURATION_LONG = 500L

    const val ENTER_TRANSLATION_Y = 48f

    const val STAGGER_STEP = 40L
}

fun View.fadeInUp(
    duration: Long = Animations.DURATION_MEDIUM,
    startDelay: Long = 0L,
    translationY: Float = Animations.ENTER_TRANSLATION_Y,
) {
    alpha = 0f
    this.translationY = translationY
    animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(startDelay)
        .setDuration(duration)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

fun View.fadeOut(
    duration: Long = Animations.DURATION_MEDIUM,
    onEnd: () -> Unit = {},
) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .setInterpolator(AccelerateInterpolator())
        .withEndAction(onEnd)
        .start()
}

fun View.crossfadeTo(
    target: View,
    duration: Long = Animations.DURATION_MEDIUM,
) {
    target.alpha = 0f
    target.visibility = View.VISIBLE
    target.animate().alpha(1f).setDuration(duration).start()
    animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction { visibility = View.GONE }
        .start()
}

fun View.pulse(duration: Long = Animations.DURATION_SHORT) {
    ObjectAnimator.ofPropertyValuesHolder(
        this,
        android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.25f, 1f),
        android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.25f, 1f),
    ).apply {
        this.duration = duration
        interpolator = OvershootInterpolator()
        start()
    }
}

fun View.staggerIn(position: Int) {
    fadeInUp(startDelay = position.coerceAtLeast(0) * Animations.STAGGER_STEP)
}
