package com.nautesh.challengeclock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownTest {
    private val fiveMin = Countdown(durationMs = 300_000)

    @Test fun startRunsFullDuration() {
        val t = fiveMin.started(now = 1_000)
        assertEquals(301_000, t.endAt)
        assertEquals(120_000, t.remainingAt(181_000))
    }

    @Test fun pauseKeepsTimeLeftAndResumeContinues() {
        val paused = fiveMin.started(0).paused(now = 60_000)
        assertEquals(240_000, paused.remainingMs)
        assertEquals(1_240_000, paused.started(now = 1_000_000).endAt)
    }

    @Test fun neverNegative() = assertEquals(0, fiveMin.started(0).remainingAt(999_999))

    @Test fun finishedRestartsFromFullDuration() {
        val done = fiveMin.started(0).finished()
        assertTrue(done.done)
        assertEquals(300_000, done.started(0).remainingMs)
    }

    @Test fun resetIsReady() = assertTrue(fiveMin.started(0).paused(5).reset().ready)

    // Started at wall 1_000 (elapsed 500), so it ends at wall 301_000 / elapsed 300_500, in boot 7.
    private val running = fiveMin.started(1_000).copy(endElapsed = 300_500, boot = 7)

    @Test fun clockSetBackAnHourKeepsTheRealEnd() =
        assertEquals(301_000 - 3_600_000, running.realigned(nowWall = 100_500 - 3_600_000, nowElapsed = 100_000, bootCount = 7).endAt)

    @Test fun otherBootOrUnknownBootLeftAlone() {
        assertEquals(301_000, running.realigned(nowWall = 50_000, nowElapsed = 10, bootCount = 8).endAt)
        assertEquals(301_000, running.copy(boot = 0).realigned(nowWall = 50_000, nowElapsed = 10, bootCount = 0).endAt)
    }
}
