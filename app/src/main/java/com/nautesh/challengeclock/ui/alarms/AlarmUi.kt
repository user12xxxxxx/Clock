package com.nautesh.challengeclock.ui.alarms

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.data.Alarm
import com.nautesh.challengeclock.data.ChallengeType
import com.nautesh.challengeclock.data.nextTrigger
import java.time.ZonedDateTime
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.Switch
import androidx.compose.foundation.selection.toggleable
import android.content.Context
import java.text.DateFormatSymbols
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** Hour/minute text and optional AM/PM marker, following the system 12/24 h setting. */
data class ClockText(val hour: String, val minute: String, val meridiem: String?)

@Composable
@ReadOnlyComposable
fun clockText(hour: Int, minute: Int, is24: Boolean): ClockText =
    if (is24) {
        ClockText("%02d".format(hour), "%02d".format(minute), null)
    } else {
        // Unpadded, as the system shows 12-hour times ("7:30 AM", not "07:30 AM").
        ClockText(
            ((hour + 11) % 12 + 1).toString(),
            "%02d".format(minute),
            meridiem(hour),
        )
    }

/** The locale's AM/PM marker for [hour] (0–23). */
fun meridiem(hour: Int): String = DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings[if (hour < 12) 0 else 1]

/** "Wednesday, 7 October", in the locale's own order and wording. */
fun longDate(date: LocalDate): String =
    android.icu.text.DateFormat.getInstanceForSkeleton("EEEEdMMMM", Locale.getDefault())
        .format(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))

/** Time until [d] ends in whole minutes, rounded up so "0 min" never shows: "45 min", "7 h", "7 h 3 min". */
fun durationText(context: Context, d: Duration): String {
    val mins = ((d.toMillis() + 59_999) / 60_000).coerceAtLeast(1)
    return when {
        mins < 60 -> context.getString(R.string.duration_min, mins)
        mins % 60 == 0L -> context.getString(R.string.duration_h, mins / 60)
        else -> context.getString(R.string.duration_h_min, mins / 60, mins % 60)
    }
}

/** "Alarm set for 7 h 3 min from now", confirming a saved alarm; null if it's off. */
fun alarmSetText(context: Context, alarm: Alarm): String? {
    val now = ZonedDateTime.now()
    val at = alarm.nextTrigger(now) ?: return null
    return context.getString(R.string.alarm_set_in, durationText(context, Duration.between(now, at)))
}

/** Days of the week in the user's locale order (Sunday-first or Monday-first). */
fun localeWeek(): List<DayOfWeek> {
    val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    return (0L until 7L).map { first.plus(it) }
}

fun DayOfWeek.bit() = 1 shl (value - 1)

private const val EVERY_DAY = 0b1111111

/** The locale's weekend as day bits: Saturday–Sunday in most places, Friday–Saturday or Friday alone in others. */
private fun weekendDays(): Int {
    val week = android.icu.util.Calendar.getInstance(Locale.getDefault()).weekData
    var mask = 0
    var day = week.weekendOnset // ICU numbering: 1 = Sunday … 7 = Saturday
    while (true) {
        mask = mask or DayOfWeek.of((day + 5) % 7 + 1).bit()
        if (day == week.weekendCease) return mask
        day = day % 7 + 1
    }
}

@Composable
@ReadOnlyComposable
fun daysLabel(days: Int): String {
    val weekend = weekendDays()
    return when (days) {
        EVERY_DAY -> stringResource(R.string.days_every)
        EVERY_DAY and weekend.inv() -> stringResource(R.string.days_weekdays)
        weekend -> stringResource(R.string.days_weekends)
        0 -> stringResource(R.string.days_once)
        else -> localeWeek()
            .filter { days and it.bit() != 0 }
            .joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
}

// Segmented list: big outer corners, tight inner ones.
fun segmentShape(index: Int, count: Int): Shape {
    val outer = 28.dp
    val inner = 6.dp
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@get:StringRes val ChallengeType.title: Int
    get() = when (this) {
        ChallengeType.MATH -> R.string.challenge_math
        ChallengeType.MEMORY -> R.string.challenge_memory
        ChallengeType.SHAPES -> R.string.challenge_shapes
        ChallengeType.RETYPE -> R.string.challenge_retype
        ChallengeType.SHAKE -> R.string.challenge_shake
    }

@get:StringRes val ChallengeType.description: Int
    get() = when (this) {
        ChallengeType.MATH -> R.string.challenge_math_desc
        ChallengeType.MEMORY -> R.string.challenge_memory_desc
        ChallengeType.SHAPES -> R.string.challenge_shapes_desc
        ChallengeType.RETYPE -> R.string.challenge_retype_desc
        ChallengeType.SHAKE -> R.string.challenge_shake_desc
    }

@get:DrawableRes private val ChallengeType.icon: Int
    get() = when (this) {
        ChallengeType.MATH -> R.drawable.ic_challenge_math
        ChallengeType.MEMORY -> R.drawable.ic_challenge_memory
        ChallengeType.SHAPES -> R.drawable.ic_challenge_shapes
        ChallengeType.RETYPE -> R.drawable.ic_challenge_retype
        ChallengeType.SHAKE -> R.drawable.ic_challenge_shake
    }

// Each challenge gets its own expressive shape and color role so the list reads at a glance.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChallengeAvatar(type: ChallengeType, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val (shape, color) = when (type) {
        ChallengeType.MATH -> MaterialShapes.Cookie9Sided to c.primaryContainer
        ChallengeType.MEMORY -> MaterialShapes.Clover8Leaf to c.tertiaryContainer
        ChallengeType.SHAPES -> MaterialShapes.SoftBurst to c.secondaryContainer
        ChallengeType.RETYPE -> MaterialShapes.Cookie4Sided to c.primary
        ChallengeType.SHAKE -> MaterialShapes.Sunny to c.tertiary
    }
    Box(
        modifier.size(48.dp).clip(shape.toShape()).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(type.icon), contentDescription = null, tint = contentColorFor(color))
    }
}

/** Switch colors from the design: when on, a light (onPrimary) track with a primary dot. */
@Composable
fun appSwitchColors(): SwitchColors = MaterialTheme.colorScheme.let { c ->
    SwitchDefaults.colors(checkedThumbColor = c.primary, checkedTrackColor = c.onPrimary, checkedBorderColor = c.onPrimary, checkedIconColor = c.onPrimary)
}

/**
 * A switch that is one accessibility node carrying [label], with a 48dp touch target
 * but laid out at the switch's drawn 32dp, so rows line up with the design.
 */
@Composable
fun LabeledSwitch(checked: Boolean, label: String, onToggle: () -> Unit, colors: SwitchColors = appSwitchColors()) {
    Box(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val h = 32.dp.roundToPx()
                layout(placeable.width, h) { placeable.place(0, (h - placeable.height) / 2) }
            }
            .minimumInteractiveComponentSize()
            // One node for TalkBack: label, role and state together; toggleable's own semantics are cleared below this.
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                onClick { onToggle(); true }
            }
            .toggleable(checked, role = Role.Switch) { onToggle() },
        contentAlignment = Alignment.Center,
    ) { Switch(checked, onCheckedChange = null, colors = colors) }
}
