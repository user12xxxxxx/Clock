package com.nautesh.challengeclock.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import com.nautesh.challengeclock.MainActivity
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.data.Countdown
import java.util.Date

/** One ongoing notification per running timer; the system chronometer does the counting. */
class TimerNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun update(timer: Countdown) {
        if (!timer.running) return cancel(timer.id)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_timers), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(context, 2, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val pause = PendingIntent.getBroadcast(
            context,
            timer.id.toInt(),
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_TIMER_PAUSE).putExtra(AlarmReceiver.EXTRA_ID, timer.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tab_timer)
            .setContentTitle(timer.label.ifBlank { context.getString(R.string.tab_timer) })
            .setWhen(timer.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.pause), pause)
            .build()
        manager.notify(BASE_ID + timer.id.toInt(), notification)
    }

    /** A timer that ran out while the app couldn't ring it (phone off, app force-stopped). */
    fun missed(timer: Countdown, endedAt: Long) {
        val open = PendingIntent.getActivity(context, 2, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tab_timer)
            .setContentTitle(timer.label.ifBlank { context.getString(R.string.tab_timer) })
            .setContentText(context.getString(R.string.timer_missed, DateFormat.getTimeFormat(context).format(Date(endedAt))))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_timers), NotificationManager.IMPORTANCE_LOW),
        )
        manager.notify(BASE_ID + timer.id.toInt(), notification)
    }

    fun cancel(id: Long) = manager.cancel(BASE_ID + id.toInt())

    private companion object {
        const val CHANNEL = "timers"
        const val BASE_ID = 1000
    }
}
