package com.nautesh.challengeclock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTriggerTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(day: Int, hour: Int, minute: Int) = ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    // 2026-10-02 is a Friday
    private val friday = at(2, 20, 0)
    private val weekdays = 0b0011111

    @Test fun oneShotLaterToday() = assertEquals(at(2, 22, 30), Alarm(hour = 22, minute = 30).nextTrigger(friday))

    @Test fun oneShotAlreadyPassedGoesTomorrow() = assertEquals(at(3, 6, 30), Alarm(hour = 6, minute = 30).nextTrigger(friday))

    @Test fun exactTriggerMomentMovesOn() = assertEquals(at(3, 20, 0), Alarm(hour = 20, minute = 0).nextTrigger(friday))

    @Test fun weekdaysFromFridayEveningIsMonday() =
        assertEquals(at(5, 6, 30), Alarm(hour = 6, minute = 30, days = weekdays).nextTrigger(friday))

    @Test fun weekdayLaterSameDay() =
        assertEquals(at(2, 21, 0), Alarm(hour = 21, minute = 0, days = weekdays).nextTrigger(friday))

    @Test fun disabledNeverRings() = assertNull(Alarm(hour = 7, minute = 0, enabled = false).nextTrigger(friday))

    private fun ms(t: ZonedDateTime) = t.toInstant().toEpochMilli()

    // A one-time alarm is off once it rings, so its snooze is the only ring left.
    @Test fun snoozeOfDisabledAlarmStillRings() =
        assertEquals(at(2, 20, 10), Alarm(hour = 19, minute = 50, enabled = false, snoozedUntil = ms(at(2, 20, 10))).nextRing(friday))

    @Test fun regularRingBeforeSnoozeWins() =
        assertEquals(at(2, 20, 5), Alarm(hour = 20, minute = 5, snoozedUntil = ms(at(2, 20, 30))).nextRing(friday))

    @Test fun pastSnoozeIgnored() =
        assertNull(Alarm(hour = 7, minute = 0, enabled = false, snoozedUntil = ms(at(2, 19, 0))).nextRing(friday))
}
