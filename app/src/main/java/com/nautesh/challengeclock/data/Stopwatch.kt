package com.nautesh.challengeclock.data

import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** [startedAt] is SystemClock.elapsedRealtime() when running; [laps] are cumulative split totals. */
data class StopwatchState(
    val running: Boolean = false,
    val baseMs: Long = 0,
    val startedAt: Long = 0,
    val laps: List<Long> = emptyList(),
) {
    fun elapsed(now: Long): Long = baseMs + if (running) (now - startedAt).coerceAtLeast(0) else 0

    fun start(now: Long) = if (running) this else copy(running = true, startedAt = now)
    fun stop(now: Long) = if (!running) this else copy(running = false, baseMs = elapsed(now), startedAt = 0)
    fun lap(now: Long) = if (running) copy(laps = laps + elapsed(now)) else this
    fun reset() = StopwatchState()

    /** Length of each recorded lap, oldest first. */
    val lapDurations: List<Long> get() = laps.mapIndexed { i, total -> total - (laps.getOrNull(i - 1) ?: 0) }

    /** Indexes of the fastest and slowest laps; only meaningful with two or more laps. */
    val fastestLap: Int? get() = lapDurations.takeIf { it.size > 1 }?.let { d -> d.indices.minBy { d[it] } }
    val slowestLap: Int? get() = lapDurations.takeIf { it.size > 1 }?.let { d -> d.indices.maxBy { d[it] } }
}

/**
 * Keeps the stopwatch in preferences so it survives the app being killed. [bootCount] is
 * Settings.Global.BOOT_COUNT: elapsedRealtime restarts at boot, so a stopwatch left running
 * across a reboot is carried over with the wall clock instead.
 */
class StopwatchStore(private val prefs: SharedPreferences, private val bootCount: Int) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<StopwatchState> = _state

    fun update(change: (StopwatchState, Long) -> StopwatchState) {
        val next = change(_state.value, SystemClock.elapsedRealtime())
        _state.value = next
        save(next)
    }

    private fun save(s: StopwatchState) {
        prefs.edit()
            .putBoolean("running", s.running)
            .putLong("base", s.baseMs)
            .putLong("startedAt", s.startedAt)
            // Wall-clock moment the run began, for carrying it over a reboot.
            .putLong("startedWall", System.currentTimeMillis() - (SystemClock.elapsedRealtime() - s.startedAt))
            .putInt("boot", bootCount)
            .putString("laps", s.laps.joinToString(","))
            .apply()
    }

    private fun load(): StopwatchState {
        val s = StopwatchState(
            running = prefs.getBoolean("running", false),
            baseMs = prefs.getLong("base", 0),
            startedAt = prefs.getLong("startedAt", 0),
            laps = prefs.getString("laps", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() },
        )
        // A start time later than "now" can only come from before a reboot: catches it where BOOT_COUNT is missing (0).
        val rebooted = prefs.getInt("boot", bootCount) != bootCount || s.startedAt > SystemClock.elapsedRealtime()
        if (!s.running || !rebooted) return s
        // ponytail: wall clock spans the reboot, so a manual clock change while off skews it
        val sinceStart = (System.currentTimeMillis() - prefs.getLong("startedWall", System.currentTimeMillis())).coerceAtLeast(0)
        return s.copy(baseMs = s.baseMs + sinceStart, startedAt = SystemClock.elapsedRealtime()).also(::save)
    }
}
