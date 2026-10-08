package com.nautesh.challengeclock.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    // Bit 0 = Monday … bit 6 = Sunday. 0 means ring once.
    val days: Int = 0,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    // Comma-separated ChallengeType ids, in the order set in the editor.
    val challenges: String = "",
    val shuffleChallenges: Boolean = false,
    val challengeCount: Int = 0,
    val muteSeconds: Int = 0,
    val restartMuteOnTouch: Boolean = false,
    // Ringtone content URI; empty means the system's default alarm sound.
    val sound: String = "",
    // How many times the alarm may be snoozed per ring; 0 means no limit.
    val snoozeLimit: Int = 0,
    // Epoch ms the pending snooze rings at; 0 = not snoozed. Kept in the row so a reboot can re-arm it.
    val snoozedUntil: Long = 0,
    // Snoozes this ring has used, carried into a re-armed snooze so the limit still holds.
    val snoozeCount: Int = 0,
) : Serializable // so the editor can keep a draft in its SavedStateHandle

fun Alarm.nextTrigger(after: ZonedDateTime): ZonedDateTime? = if (enabled) nextTrigger(hour, minute, days, after) else null

fun Alarm.snoozedAt(now: ZonedDateTime) = snoozedUntil > now.toInstant().toEpochMilli()

/** Next time this alarm rings: its pending snooze or its next regular time, whichever comes first. */
fun Alarm.nextRing(after: ZonedDateTime): ZonedDateTime? {
    val snooze = if (snoozedAt(after)) ZonedDateTime.ofInstant(Instant.ofEpochMilli(snoozedUntil), after.zone) else null
    return listOfNotNull(snooze, nextTrigger(after)).minOrNull()
}

/** Next time [hour]:[minute] falls strictly after [after] on one of [days] (bit 0 = Monday; 0 = any day). */
fun nextTrigger(hour: Int, minute: Int, days: Int, after: ZonedDateTime): ZonedDateTime? {
    val time = LocalTime.of(hour, minute)
    for (offset in 0L..7L) {
        val candidate = ZonedDateTime.of(after.toLocalDate().plusDays(offset), time, after.zone)
        if (candidate.isAfter(after) && (days == 0 || days and (1 shl (candidate.dayOfWeek.value - 1)) != 0)) return candidate
    }
    return null
}
