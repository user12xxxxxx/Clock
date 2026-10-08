package com.nautesh.challengeclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nautesh.challengeclock.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Fires alarms and timers, pauses timers and dismisses snoozes from their notifications, and re-arms everything after reboot, app update, clock/timezone change or exact-alarm grant.
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarms = context.app.alarms
        val timers = context.app.timers
        val id = intent.getLongExtra(EXTRA_ID, -1)
        when (intent.action) {
            ACTION_FIRE -> {
                context.startForegroundService(
                    RingingService.intent(context, id).putExtra(EXTRA_SNOOZE_COUNT, intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)),
                )
                inBackground { if (intent.getBooleanExtra(EXTRA_SNOOZE, false)) alarms.clearSnooze(id) else alarms.onRang(id) }
            }
            ACTION_DISMISS_SNOOZE -> inBackground { alarms.clearSnooze(id) }
            ACTION_TIMER_DONE -> {
                context.startForegroundService(RingingService.intent(context, id, RingingService.ACTION_TIMER))
                inBackground { timers.onFired(id) }
            }
            ACTION_TIMER_PAUSE -> inBackground { timers.pause(id) }
            else -> inBackground {
                alarms.rescheduleAll(afterBoot = intent.action == Intent.ACTION_BOOT_COMPLETED)
                timers.rescheduleAll()
            }
        }
    }

    private fun inBackground(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.nautesh.challengeclock.FIRE"
        const val ACTION_TIMER_DONE = "com.nautesh.challengeclock.TIMER_DONE"
        const val ACTION_TIMER_PAUSE = "com.nautesh.challengeclock.TIMER_PAUSE"
        const val ACTION_DISMISS_SNOOZE = "com.nautesh.challengeclock.DISMISS_SNOOZE"
        const val EXTRA_ID = "alarm_id"
        const val EXTRA_SNOOZE = "snooze"
        const val EXTRA_SNOOZE_COUNT = "snooze_count"
    }
}
