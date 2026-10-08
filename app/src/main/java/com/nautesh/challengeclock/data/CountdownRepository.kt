package com.nautesh.challengeclock.data

import android.os.SystemClock
import com.nautesh.challengeclock.alarm.AlarmScheduler
import com.nautesh.challengeclock.alarm.TimerNotifier

/**
 * Every change goes through [save], which keeps the AlarmManager wake-up and the countdown
 * notification in step with the row. [stopRinging] silences a finished timer that is cleared.
 */
class CountdownRepository(
    private val dao: CountdownDao,
    private val scheduler: AlarmScheduler,
    private val notifier: TimerNotifier,
    private val bootCount: Int,
    private val stopRinging: (Long) -> Unit,
) {
    val all = dao.observeAll()

    suspend fun get(id: Long) = dao.get(id)

    suspend fun add(durationMs: Long, label: String): Countdown =
        save(Countdown(label = label, durationMs = durationMs).started(now()))

    suspend fun toggle(t: Countdown) = save(if (t.running) t.paused(now()) else t.started(now()))

    suspend fun pause(id: Long) {
        dao.get(id)?.takeIf { it.running }?.let { save(it.paused(now())) }
    }

    suspend fun reset(t: Countdown) = save(t.reset())

    suspend fun delete(t: Countdown) {
        scheduler.cancelTimer(t.id)
        notifier.cancel(t.id)
        stopRinging(t.id)
        dao.delete(t)
    }

    suspend fun onFired(id: Long) {
        dao.get(id)?.takeIf { it.running }?.let { save(it.finished()) }
    }

    /** Restores a removed timer (Undo); a running one whose end has passed meanwhile rings right away. */
    suspend fun restore(t: Countdown) = save(t)

    // Runs on app start and after reboot, clock or time-zone change. Ends are first put back where the wall clock
    // change moved them. Future ends are re-armed. One that ended over a minute ago was missed (reboot, force-stop):
    // it is marked done and the user told so. One that ended moments ago is ringing right now, through the broadcast
    // that started the app, and is left to that broadcast.
    suspend fun rescheduleAll() {
        val now = now()
        val elapsed = SystemClock.elapsedRealtime()
        dao.getAll().filter { it.running }.map { it.realigned(now, elapsed, bootCount) }.forEach {
            when {
                it.endAt > now -> save(it)
                it.endAt < now - MISSED_AFTER_MS -> notifier.missed(save(it.finished()), it.endAt)
            }
        }
    }

    private suspend fun save(t: Countdown): Countdown {
        // Stamp the same end in elapsed time, which a wall-clock change can't move.
        val row = if (t.running) t.copy(endElapsed = SystemClock.elapsedRealtime() + (t.endAt - now()), boot = bootCount) else t
        val saved = row.copy(id = dao.put(row))
        if (saved.running) scheduler.scheduleTimer(saved.id, saved.endAt) else scheduler.cancelTimer(saved.id)
        notifier.update(saved)
        if (!saved.done) stopRinging(saved.id)
        return saved
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        const val MISSED_AFTER_MS = 60_000L
    }
}
