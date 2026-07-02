package hr.alg.iamu_project_bp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hr.alg.iamu_project_bp.work.SyncScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        SyncScheduler.schedulePeriodic(context.applicationContext)
    }
}
