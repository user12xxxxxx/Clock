package com.nautesh.challengeclock.ui.timers

import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.ui.AppText
import com.nautesh.challengeclock.ui.EmptyState
import com.nautesh.challengeclock.ui.LABEL_MAX_LENGTH
import com.nautesh.challengeclock.ui.PillTextField
import com.nautesh.challengeclock.ui.PrimaryButton
import com.nautesh.challengeclock.ui.SheetTitle
import com.nautesh.challengeclock.app
import com.nautesh.challengeclock.data.Countdown
import com.nautesh.challengeclock.ui.clockDigits
import com.nautesh.challengeclock.ui.FabAction
import com.nautesh.challengeclock.ui.FabMenu
import com.nautesh.challengeclock.ui.ScreenHeader
import com.nautesh.challengeclock.ui.rememberSoundPicker
import com.nautesh.challengeclock.ui.soundTitle
import com.nautesh.challengeclock.ui.LocalSnackbar
import com.nautesh.challengeclock.ui.TimeField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nautesh.challengeclock.ui.alarms.durationText
import java.time.Duration
import java.util.Date

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimerScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val repo = context.app.timers
    val scope = context.app.scope
    val snackbar = LocalSnackbar.current
    val loaded by repo.all.collectAsState(null)
    val timers = loaded.orEmpty()
    val anyRunning = timers.any { it.running }
    // Tick only while something is counting.
    val now by produceState(System.currentTimeMillis(), anyRunning) {
        value = System.currentTimeMillis()
        while (anyRunning) {
            delay(100)
            value = System.currentTimeMillis()
        }
    }
    var customOpen by rememberSaveable { mutableStateOf(false) }

    val running = timers.filter { it.running }
    val subtitle = if (running.isEmpty()) {
        if (timers.isEmpty()) null else stringResource(R.string.no_timers_running)
    } else {
        val soonest = running.minOf { it.remainingAt(now) }
        stringResource(
            R.string.timers_running,
            running.size,
            // Same "7 h 3 min" wording as the Alarms subtitle.
            if (soonest <= 60_000) stringResource(R.string.under_a_minute) else durationText(context, Duration.ofMillis(soonest)),
        )
    }
    val act: (suspend () -> Unit) -> Unit = { block -> scope.launch { block() } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Box {
            // Two columns on a phone; landscape has no hero pane here (every timer is a tile), so the grid just widens
            // beside the rail.
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(
                    start = 16.dp + contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = 16.dp + contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                    top = top + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 112.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    val app = LocalContext.current.app
                    var timerSound by remember { mutableStateOf(app.timerSound) }
                    val pickSound = rememberSoundPicker(timerSound) {
                        timerSound = it
                        app.timerSound = it
                        scope.launch { snackbar?.show(resources.getString(R.string.timer_sound_set, soundTitle(context, it))) } // title lookup is a query: off main
                    }
                    ScreenHeader(stringResource(R.string.tab_timer_title), subtitle) {
                        IconButton(pickSound) { Icon(painterResource(R.drawable.ic_music), stringResource(R.string.timer_sound)) }
                    }
                }
                // Only once the list has loaded: an empty first frame shouldn't flash "No timers yet".
                if (loaded?.isEmpty() == true) {
                    item(span = StaggeredGridItemSpan.FullLine) { EmptyState(stringResource(R.string.no_timers)) }
                }
                // No big card: every timer is a tile. Swipe a tile left to remove it (as on Clock), with Undo.
                items(timers, key = { it.id }) { t ->
                    val dismiss = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismiss.currentValue) {
                        if (dismiss.currentValue != SwipeToDismissBoxValue.EndToStart) return@LaunchedEffect
                        scope.launch { repo.delete(t) }
                        snackbar?.show(resources.getString(R.string.timer_removed), resources.getString(R.string.undo)) {
                            scope.launch { repo.restore(t) }
                        }
                    }
                    SwipeToDismissBox(
                        modifier = Modifier.animateItem(),
                        state = dismiss,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.errorContainer))
                        },
                    ) {
                        TimerTile(t, now, onToggle = { act { repo.toggle(t) } }, onReset = { act { repo.reset(t) } })
                    }
                }
            }
            CreateTimerMenu(
                onPreset = { minutes ->
                    act { repo.add(minutes * 60_000L, "") }
                },
                onCustom = { customOpen = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(contentPadding),
            )
        }
    }

    if (customOpen) {
        CustomTimerSheet(
            onDismiss = { customOpen = false },
            onStart = { ms, label ->
                customOpen = false
                act { repo.add(ms, label) }
            },
        )
    }
}

private fun clock(ms: Long): Pair<String, String> {
    val secs = (ms + 999) / 1000
    val h = secs / 3600
    val m = secs % 3600 / 60
    val s = secs % 60
    return (if (h > 0) "$h:%02d".format(m) else "%02d".format(m)) to "%02d".format(s)
}

@Composable
// A timer's set length to the second ("1 h 5 min 30 s"), unlike the rounded-up durationText.
private fun lengthText(ms: Long): String {
    val secs = ms / 1000
    val parts = listOfNotNull(
        (secs / 3600).takeIf { it > 0 }?.let { stringResource(R.string.duration_h, it) },
        (secs % 3600 / 60).takeIf { it > 0 }?.let { stringResource(R.string.duration_min, it) },
        (secs % 60).takeIf { it > 0 }?.let { stringResource(R.string.duration_s, it) },
    )
    return parts.joinToString(" ").ifEmpty { stringResource(R.string.duration_s, 0) }
}

// System time format, so "ends at" follows the 12/24-hour setting like the rest of the app.
@Composable
private fun endTime(at: Long): String = DateFormat.getTimeFormat(LocalContext.current).format(Date(at))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerTile(t: Countdown, now: Long, onToggle: () -> Unit, onReset: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(
        when { t.running -> c.primary; t.done -> c.error; else -> c.surfaceContainerLow }, effects,
    )
    val content by animateColorAsState(
        when { t.running -> c.onPrimary; t.done -> c.onError; else -> c.onSurfaceVariant }, effects,
    )
    val weight by animateIntAsState(if (t.running) 800 else 400, MaterialTheme.motionScheme.defaultEffectsSpec())
    val (main, secs) = clock(t.remainingAt(now))
    val label = t.label.ifBlank { stringResource(R.string.tab_timer) }
    val sub = when {
        t.running -> stringResource(R.string.ends_at, endTime(t.endAt))
            .replaceFirstChar { it.titlecase() }
        t.done -> stringResource(R.string.times_up)
        else -> lengthText(t.durationMs)
    }

    // Digits share the top row with the buttons; the tile grows with an hours line and the staggered grid fills the gap.
    Surface(onClick = onToggle, shape = RoundedCornerShape(28.dp), color = container, contentColor = content, modifier = Modifier.heightIn(min = 196.dp)) {
        Column(Modifier.padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Row {
                // Hours get their own line (10 / 04 / 58): "10:04" is too wide for the tile and wraps mid-number.
                Text(
                    "${main.replace(':', '\n')}\n$secs",
                    Modifier.weight(1f),
                    style = clockDigits(58.sp, width = 38f).copy(lineHeight = 53.sp, letterSpacing = (-1).sp, fontWeight = FontWeight(weight)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                    // Round when idle, morphs to a rounded square while running.
                    FilledIconToggleButton(
                        checked = t.running,
                        onCheckedChange = { onToggle() },
                        shapes = IconButtonDefaults.toggleableShapes(),
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = c.surfaceContainerHighest,
                            contentColor = c.onSurface,
                            checkedContainerColor = c.onPrimary,
                            checkedContentColor = c.primary,
                        ),
                    ) {
                        Icon(
                            painterResource(if (t.running) R.drawable.ic_pause else R.drawable.ic_play),
                            stringResource(if (t.running) R.string.pause_timer else R.string.start_timer, label),
                            Modifier.size(20.dp),
                        )
                    }
                    OutlinedIconButton(onReset, enabled = !t.ready, border = BorderStroke(1.5.dp, content)) {
                        Icon(painterResource(R.drawable.ic_reset), stringResource(R.string.reset_timer, label), Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(label, Modifier.padding(top = 8.dp), style = AppText.cardLabel.copy(fontWeight = FontWeight(weight)))
            Text(
                sub,
                Modifier.padding(top = 2.dp),
                style = AppText.cardDetail,
                color = content.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun CreateTimerMenu(onPreset: (Int) -> Unit, onCustom: () -> Unit, modifier: Modifier = Modifier) {
    FabMenu(
        listOf(FabAction(R.drawable.ic_tune, stringResource(R.string.custom_timer), onClick = onCustom)) +
            listOf(25, 10, 5, 1).map { minutes -> FabAction(R.drawable.ic_tab_timer, stringResource(R.string.preset_timer, minutes)) { onPreset(minutes) } },
        modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTimerSheet(onDismiss: () -> Unit, onStart: (Long, String) -> Unit) {
    var h by rememberSaveable { mutableStateOf("00") }
    var m by rememberSaveable { mutableStateOf("05") }
    var s by rememberSaveable { mutableStateOf("00") }
    var label by rememberSaveable { mutableStateOf("") }
    fun clamp(text: String, max: Int) = text.filter(Char::isDigit).takeLast(2).let { d ->
        d.toIntOrNull()?.let { if (it > max) "%02d".format(max) else d } ?: ""
    }
    val totalMs = ((h.toIntOrNull() ?: 0) * 3600L + (m.toIntOrNull() ?: 0) * 60L + (s.toIntOrNull() ?: 0)) * 1000
    val pad: (String) -> String = { "%02d".format(it.toIntOrNull() ?: 0) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
          SheetTitle(stringResource(R.string.custom_timer_title))
          Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Top) {
                TimeField(h, { h = clamp(it, 23) }, { h = pad(h) }, stringResource(R.string.hour), width = 92.dp, fontSize = 56.sp)
                Text(":", Modifier.padding(horizontal = 4.dp), style = clockDigits(56.sp).copy(lineHeight = 96.sp))
                TimeField(m, { m = clamp(it, 59) }, { m = pad(m) }, stringResource(R.string.minute), width = 92.dp, fontSize = 56.sp)
                Text(":", Modifier.padding(horizontal = 4.dp), style = clockDigits(56.sp).copy(lineHeight = 96.sp))
                TimeField(s, { s = clamp(it, 59) }, { s = pad(s) }, stringResource(R.string.seconds_label), width = 92.dp, fontSize = 56.sp)
            }
            PillTextField(
                value = label,
                onValueChange = { label = it.take(LABEL_MAX_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.tab_timer),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            PrimaryButton(
                stringResource(R.string.start),
                { onStart(totalMs, label.trim()) },
                Modifier.fillMaxWidth(),
                enabled = totalMs > 0,
                icon = R.drawable.ic_play,
            )
          }
        }
    }
}
