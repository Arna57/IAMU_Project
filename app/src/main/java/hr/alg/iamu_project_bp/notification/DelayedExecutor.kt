package hr.alg.iamu_project_bp.notification

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock

object DelayedExecutor {
    private val thread = HandlerThread("weather-delayed-executor").apply { start() }

    private val handler = Handler(thread.looper)

    fun postDelayed(delayMs: Long, token: Any? = null, action: () -> Unit) {
        val runnable = Runnable { action() }
        if (token != null) {
            handler.postAtTime(runnable, token, SystemClock.uptimeMillis() + delayMs)
        } else {
            handler.postDelayed(runnable, delayMs)
        }
    }

    fun cancel(token: Any) {
        handler.removeCallbacksAndMessages(token)
    }
}
