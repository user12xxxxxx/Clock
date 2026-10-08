package com.nautesh.challengeclock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StopwatchTest {
    @Test fun runsAndPauses() {
        val s = StopwatchState().start(1_000)
        assertEquals(500, s.elapsed(1_500))
        val stopped = s.stop(2_000)
        assertEquals(1_000, stopped.elapsed(99_999))
        assertEquals(1_700, stopped.start(5_000).elapsed(5_700))
    }

    @Test fun lapsAndStats() {
        val s = StopwatchState().start(0).lap(10_000).lap(17_000).lap(30_000)
        assertEquals(listOf(10_000L, 7_000L, 13_000L), s.lapDurations)
        assertEquals(1, s.fastestLap)
        assertEquals(2, s.slowestLap)
    }

    @Test fun singleLapHasNoStats() = assertNull(StopwatchState().start(0).lap(5).fastestLap)

    @Test fun lapIgnoredWhenStopped() = assertEquals(0, StopwatchState().lap(10).laps.size)
}
