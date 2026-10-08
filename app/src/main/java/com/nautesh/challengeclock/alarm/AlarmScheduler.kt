package com.nautesh.challengeclock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nautesh.challengeclock.MainActivity
import com.nautesh.challengeclock.data.Alarm
import com.nautesh.challengeclock.data.nextTrigger
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact() = alarmManager.canScheduleExactAlarms()

    fun schedule(alarm: Alarm, after: ZonedDateTime = ZonedDateTime.now()) {
        val next = alarm.nextTrigger(after) ?: return cancel(alarm.id)
        set(alarm.id, snooze = false, next.toInstant().toEpochMilli())
    }

    /** [count] is how many snoozes this ring will have used once it fires again. */
    fun snooze(id: Long, atMillis: Long, count: Int = 1) = set(id, snooze = true, atMillis, count)

    fun cancelSnooze(id: Long) = alarmManager.cancel(fireIntent(id, snooze = true))

    fun cancel(id: Long) {
        alarmManager.cancel(fireIntent(id, snooze = false))
        cancelSnooze(id)
    }

    // Timers don't show the status-bar alarm icon, so they use a plain exact wake-up.
    fun scheduleTimer(id: Long, atMillis: Long) {
        // Without exact-alarm access the timer may ring a few minutes late; better than never.
        if (canScheduleExact()) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, timerIntent(id))
        else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, timerIntent(id))
    }

    fun cancelTimer(id: Long) = alarmManager.cancel(timerIntent(id))

    // Distinct action keeps these PendingIntents apart from alarm ones with the same request code.
    private fun timerIntent(id: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.toInt(),
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_TIMER_DONE).putExtra(AlarmReceiver.EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun set(id: Long, snooze: Boolean, atMillis: Long, count: Int = 0) {
        if (!canScheduleExact()) return
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, show), fireIntent(id, snooze, count))
    }

    // Snooze gets its own request code so it never replaces the alarm's next regular ring.
    private fun fireIntent(id: Long, snooze: Boolean, count: Int = 0): PendingIntent = PendingIntent.getBroadcast(
        context,
        (id * 2 + if (snooze) 1 else 0).toInt(),
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ID, id)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE, snooze)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, count),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
