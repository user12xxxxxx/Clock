package com.nautesh.challengeclock.data

import com.nautesh.challengeclock.alarm.AlarmScheduler
import com.nautesh.challengeclock.alarm.SnoozeNotifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime

class AlarmRepository(private val dao: AlarmDao, private val scheduler: AlarmScheduler, private val snoozes: SnoozeNotifier) {
    // One writer at a time: a cold start's rescheduleAll() runs alongside the receiver that woke the app,
    // and both read a row before arming or cancelling it.
    private val lock = Mutex()

    val all = dao.observeAll()

    suspend fun get(id: Long) = dao.get(id)

    suspend fun save(alarm: Alarm): Alarm = lock.withLock { put(alarm) }

    suspend fun delete(alarm: Alarm) = lock.withLock {
        scheduler.cancel(alarm.id)
        snoozes.cancel(alarm.id)
        dao.delete(alarm)
    }

    /** Snoozes [id] for its snooze length; [count] is how many snoozes this ring will have used. */
    suspend fun snooze(id: Long, count: Int) = lock.withLock {
        val alarm = dao.get(id) ?: return@withLock // deleted while it rang
        val snoozed = alarm.copy(snoozedUntil = System.currentTimeMillis() + alarm.snoozeMinutes * 60_000L, snoozeCount = count)
        dao.setSnooze(id, snoozed.snoozedUntil, count)
        scheduler.snooze(id, snoozed.snoozedUntil, count)
        snoozes.show(snoozed)
    }

    /** The snooze rang, or was dismissed ahead of time. */
    suspend fun clearSnooze(id: Long) = lock.withLock {
        scheduler.cancelSnooze(id)
        dao.setSnooze(id, 0, 0)
        snoozes.cancel(id)
    }

    // Regular rings: enabled alarms only (a one-time alarm is disabled as soon as it rings).
    // Snoozes: a future one is re-armed (force-stop and reboot wipe AlarmManager). After a reboot, one that came
    // due while the phone was off rings now, unless it's over an hour stale. Otherwise an overdue snooze is
    // ringing right now, through the very broadcast that started the app, and is left alone.
    suspend fun rescheduleAll(afterBoot: Boolean = false) = lock.withLock {
        val now = System.currentTimeMillis()
        dao.getAll().forEach { a ->
            if (a.enabled) scheduler.schedule(a)
            when {
                a.snoozedUntil > now -> {
                    scheduler.snooze(a.id, a.snoozedUntil, a.snoozeCount)
                    snoozes.show(a)
                }
                a.snoozedUntil == 0L || !afterBoot -> Unit
                a.snoozedUntil > now - STALE_SNOOZE_MS -> scheduler.snooze(a.id, now, a.snoozeCount)
                else -> {
                    dao.setSnooze(a.id, 0, 0)
                    snoozes.cancel(a.id)
                }
            }
        }
    }

    suspend fun onRang(id: Long) = lock.withLock {
        val alarm = dao.get(id) ?: return@withLock
        if (alarm.days == 0) {
            put(alarm.copy(enabled = false))
        } else {
            // Start a minute ahead so an alarm delivered a hair early can't re-arm for the same minute.
            scheduler.schedule(alarm, ZonedDateTime.now().plusMinutes(1))
        }
    }

    // The snooze columns belong to the ringing flow: an edit made from an older copy keeps whatever snooze is
    // pending now, and turning an alarm off cancels its snooze along with it (schedule() cancels both rings).
    private suspend fun put(alarm: Alarm): Alarm {
        val stored = dao.get(alarm.id)
        val row = if (alarm.enabled) {
            alarm.copy(snoozedUntil = stored?.snoozedUntil ?: 0, snoozeCount = stored?.snoozeCount ?: 0)
        } else {
            alarm.copy(snoozedUntil = 0, snoozeCount = 0)
        }
        val saved = row.copy(id = dao.put(row))
        scheduler.schedule(saved)
        if (!saved.enabled) snoozes.cancel(saved.id)
        return saved
    }

    private companion object {
        const val STALE_SNOOZE_MS = 60 * 60_000L
    }
}
