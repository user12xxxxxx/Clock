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
import com.nautesh.challengeclock.data.Alarm
import java.util.Date

/** "Snoozed until 7:10" with a Dismiss action, for as long as an alarm's snooze is pending. */
class SnoozeNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun show(alarm: Alarm) {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_snoozed), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(context, 3, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val dismiss = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_DISMISS_SNOOZE).putExtra(AlarmReceiver.EXTRA_ID, alarm.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val until = DateFormat.getTimeFormat(context).format(Date(alarm.snoozedUntil))
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(alarm.label.ifBlank { context.getString(R.string.alarm) })
            .setContentText(context.getString(R.string.snoozed_until, until))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.dismiss), dismiss)
            .build()
        manager.notify(BASE_ID + alarm.id.toInt(), notification)
    }

    fun cancel(id: Long) = manager.cancel(BASE_ID + id.toInt())

    private companion object {
        const val CHANNEL = "snoozed"
        const val BASE_ID = 1_000_000 // clear of the ringing (1) and timer (1000 + id) notifications
    }
}
