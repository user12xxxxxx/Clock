package com.nautesh.challengeclock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A timer. While running only [endAt] matters; otherwise [remainingMs] holds the time left.
 * Ready = remaining equals duration, paused = some left, done = none left.
 */
@Entity(tableName = "timers")
data class Countdown(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val durationMs: Long,
    val remainingMs: Long = durationMs,
    val endAt: Long = 0,
    val running: Boolean = false,
    // While running: the end as SystemClock.elapsedRealtime(), and the boot (Settings.Global.BOOT_COUNT) it
    // belongs to. Unlike the wall-clock [endAt] these don't move when the clock is changed; see [realigned].
    val endElapsed: Long = 0,
    val boot: Int = 0,
) {
    fun remainingAt(now: Long): Long = if (running) (endAt - now).coerceAtLeast(0) else remainingMs
    val done: Boolean get() = !running && remainingMs == 0L
    val ready: Boolean get() = !running && remainingMs == durationMs

    fun started(now: Long): Countdown {
        val left = if (remainingMs == 0L) durationMs else remainingMs
        return copy(running = true, remainingMs = left, endAt = now + left)
    }

    fun paused(now: Long) = copy(running = false, remainingMs = remainingAt(now), endAt = 0)

    fun reset() = copy(running = false, remainingMs = durationMs, endAt = 0)

    fun finished() = copy(running = false, remainingMs = 0, endAt = 0)

    /**
     * After the wall clock was changed, puts [endAt] back to the same real moment, from [endElapsed]. Only within
     * the boot that set it (elapsed time restarts at boot) and only when the boot count is known (0 = unknown).
     */
    fun realigned(nowWall: Long, nowElapsed: Long, bootCount: Int): Countdown =
        if (running && bootCount != 0 && boot == bootCount) copy(endAt = nowWall + (endElapsed - nowElapsed)) else this
}
