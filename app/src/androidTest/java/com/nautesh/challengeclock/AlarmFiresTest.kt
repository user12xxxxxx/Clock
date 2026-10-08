package com.nautesh.challengeclock

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nautesh.challengeclock.alarm.RingingService
import com.nautesh.challengeclock.data.Alarm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// End to end through the real AlarmManager, receiver and ringing service.
@RunWith(AndroidJUnit4::class)
class AlarmFiresTest {
    @Test fun scheduledAlarmRings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Disabled so the alarm itself never arms; a snooze due now fires it right away.
        val alarm = context.app.alarms.save(Alarm(hour = 0, minute = 0, label = "Instrumented test", enabled = false))
        try {
            context.app.scheduler.snooze(alarm.id, System.currentTimeMillis())
            val rang = withTimeoutOrNull(15_000) { RingingService.ringingId.first { it == alarm.id } }
            assertEquals(alarm.id, rang)
        } finally {
            context.startService(RingingService.intent(context, alarm.id, RingingService.ACTION_STOP))
            context.app.alarms.delete(alarm)
        }
    }

    @Test fun snoozeLimitReachedDisablesSnooze() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarm = context.app.alarms.save(Alarm(hour = 0, minute = 0, label = "Instrumented test", enabled = false, snoozeLimit = 1))
        try {
            // Fires as the first snooze coming back, so the one allowed snooze is used up.
            context.app.scheduler.snooze(alarm.id, System.currentTimeMillis(), count = 1)
            withTimeoutOrNull(15_000) { RingingService.ringingId.first { it == alarm.id } }
            val blocked = withTimeoutOrNull(5_000) { RingingService.canSnooze.first { !it } }
            assertEquals(false, blocked)
        } finally {
            context.startService(RingingService.intent(context, alarm.id, RingingService.ACTION_STOP))
            context.app.alarms.delete(alarm)
        }
    }
}
