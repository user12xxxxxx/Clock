package com.nautesh.challengeclock.ui.ringing

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.ui.AppText
import com.nautesh.challengeclock.ui.PrimaryButton
import com.nautesh.challengeclock.data.Alarm
import com.nautesh.challengeclock.data.ChallengeType
import com.nautesh.challengeclock.ui.LocalReduceMotion
import com.nautesh.challengeclock.ui.alarms.clockText
import com.nautesh.challengeclock.ui.alarms.longDate
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import com.nautesh.challengeclock.ui.clockDigits
import com.nautesh.challengeclock.ui.flexText
import com.nautesh.challengeclock.ui.isLandscape
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * Ringing → (Stop) → each planned challenge in turn → dismissed.
 * [onChallengesStarted] fires once when solving begins; [onInteract] on every touch while solving.
 * [plan] is null while the alarm is still loading; Stop stays disabled until then so a quick tap can't skip the challenges.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RingingScreen(
    alarm: Alarm?,
    plan: List<ChallengeType>?,
    canSnooze: Boolean,
    muted: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onChallengesStarted: () -> Unit,
    onInteract: () -> Unit,
) {
    var solving by rememberSaveable { mutableStateOf(false) }
    var solved by rememberSaveable { mutableIntStateOf(0) }
    val steps = plan.orEmpty()
    BackHandler {} // no backing out of a ringing alarm

    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.primaryContainer, contentColor = c.onPrimaryContainer) {
        AnimatedContent(solving, label = "phase") { inChallenges ->
            if (!inChallenges) {
                RingingFace(alarm, plan?.size, canSnooze, onSnooze) {
                    if (plan == null) return@RingingFace
                    if (plan.isEmpty()) onDismiss() else { solving = true; onChallengesStarted() }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .imePadding()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                    onInteract()
                                }
                            }
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text(
                        stringResource(R.string.challenge_progress, solved + 1, steps.size),
                        style = AppText.sectionTitle,
                    )
                    LinearWavyProgressIndicator(
                        progress = { solved / steps.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = c.onPrimaryContainer,
                        trackColor = c.surfaceContainerLowest,
                        amplitude = if (LocalReduceMotion.current) { _ -> 0f } else { _ -> 1f },
                    )
                    if (muted) { // only while actually silent, not after the mute runs out
                        Text(stringResource(R.string.muted_hint), style = AppText.hint)
                    }
                    Spacer(Modifier.height(8.dp))
                    // key() gives each challenge fresh state.
                    key(solved) {
                        Challenge(steps[solved]) {
                            if (solved + 1 == steps.size) onDismiss() else solved++
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RingingFace(alarm: Alarm?, challengeCount: Int?, canSnooze: Boolean, onSnooze: () -> Unit, onStop: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    // The time now, not the alarm's set time: after a snooze those differ (7:20 vs 7:00). Ticks on the minute.
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            value = LocalDateTime.now()
        }
    }
    val date = longDate(now.toLocalDate())
    val time = clockText(now.hour, now.minute, is24)
    // Landscape: date, ringing shape, time and label on the left; Snooze and Stop in a 300dp column on the right.
    val landscape = isLandscape()

    val blob = @Composable { size: Dp, iconSize: Dp ->
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            if (LocalReduceMotion.current) {
                Box(Modifier.size(size).clip(MaterialShapes.Cookie9Sided.toShape()).background(c.primary))
            } else {
                LoadingIndicator(Modifier.size(size), color = c.primary)
            }
            Icon(painterResource(R.drawable.ic_tab_alarm), null, Modifier.size(iconSize), tint = c.onPrimary)
        }
    }
    val clock = @Composable { modifier: Modifier, fontSize: TextUnit ->
        Row(modifier, verticalAlignment = Alignment.Bottom) {
            // Wide (width 112) heavy digits, as designed; AM/PM still shows on 12-hour phones.
            // Shrinks to fit: "12:00" this wide, plus AM/PM, can outgrow a narrow phone.
            BasicText(
                "${time.hour}:${time.minute}",
                Modifier.weight(1f, fill = false),
                style = clockDigits(fontSize, width = 112f).copy(lineHeight = fontSize, letterSpacing = (-3).sp, fontWeight = FontWeight(900), color = LocalContentColor.current),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 48.sp, maxFontSize = fontSize),
            )
            if (time.meridiem != null) Text(time.meridiem, Modifier.padding(start = 6.dp, bottom = 16.dp), style = MaterialTheme.typography.headlineSmall)
        }
    }
    val label = @Composable { modifier: Modifier ->
        if (alarm != null) Text(alarm.label.ifBlank { stringResource(R.string.alarm) }, modifier, style = AppText.statement)
    }
    val chip = @Composable { modifier: Modifier ->
        if (challengeCount != null && challengeCount > 0) {
            Surface(modifier, shape = CircleShape, color = c.surfaceContainerLowest, contentColor = c.onSurface) {
                Text(
                    if (challengeCount == 1) stringResource(R.string.solve_one_to_stop) else stringResource(R.string.solve_to_stop, challengeCount),
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        }
    }
    val buttons = @Composable {
        FilledTonalButton(
            onClick = onSnooze,
            shapes = ButtonShapes(shape = RoundedCornerShape(32.dp), pressedShape = RoundedCornerShape(16.dp)),
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = canSnooze,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.surfaceContainerLowest, contentColor = c.onSurface),
        ) {
            Text(
                if (canSnooze) stringResource(R.string.snooze_minutes, alarm?.snoozeMinutes ?: 10) else stringResource(R.string.no_snoozes_left),
                style = AppText.prominent,
            )
        }
        Button(
            onClick = onStop,
            enabled = challengeCount != null,
            shapes = ButtonShapes(shape = RoundedCornerShape(68.dp), pressedShape = RoundedCornerShape(28.dp)),
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(136.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.onPrimaryContainer, contentColor = c.primaryContainer),
        ) { Text(stringResource(R.string.stop), style = flexText(48.sp, width = 115f, weight = 850)) }
    }

    if (landscape) {
        Row(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    blob(112.dp, 40.dp)
                    Text(date, Modifier.padding(start = 16.dp), style = AppText.prominent)
                }
                clock(Modifier.padding(top = 12.dp), 104.sp)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    label(Modifier.padding(end = 12.dp))
                    chip(Modifier)
                }
            }
            Column(Modifier.width(300.dp)) { buttons() }
        }
        return
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Only the top scrolls: Snooze and Stop stay pinned and reachable on short phones and at large display sizes,
      // where the whole face (~750dp) doesn't fit.
      Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(date, style = AppText.prominent, textAlign = TextAlign.Center)
        Box(Modifier.padding(top = 24.dp)) { blob(200.dp, 64.dp) }
        clock(Modifier.padding(top = 32.dp), 112.sp)
        label(Modifier.padding(top = 12.dp))
        chip(Modifier.padding(top = 16.dp))
      }
        Spacer(Modifier.height(12.dp))
        buttons()
    }
}

/** Shown once the alarm is stopped from this screen, as in the design. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmDismissed(onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.primaryContainer, contentColor = c.onPrimaryContainer) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(R.string.alarm_dismissed), style = AppText.statement)
            PrimaryButton(stringResource(R.string.back_to_alarms), onBack)
        }
    }
}
