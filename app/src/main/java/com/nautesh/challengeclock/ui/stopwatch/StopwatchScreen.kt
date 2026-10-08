package com.nautesh.challengeclock.ui.stopwatch

import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.ui.AppText
import com.nautesh.challengeclock.ui.EmptyState
import com.nautesh.challengeclock.ui.SectionHeader
import com.nautesh.challengeclock.app
import com.nautesh.challengeclock.ui.LocalReduceMotion
import com.nautesh.challengeclock.ui.LocalSnackbar
import com.nautesh.challengeclock.ui.ScreenHeader
import com.nautesh.challengeclock.ui.TwoPane
import com.nautesh.challengeclock.ui.FullScreenTime
import com.nautesh.challengeclock.ui.FullScreenOption
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.nautesh.challengeclock.ui.isLandscape
import com.nautesh.challengeclock.ui.alarms.segmentShape
import com.nautesh.challengeclock.ui.clockDigits

private fun main(ms: Long): String {
    val h = ms / 3_600_000
    val m = ms / 60_000 % 60
    val s = ms / 1000 % 60
    return if (h > 0) "$h:%02d:%02d".format(m, s) else "%02d:%02d".format(m, s)
}

private fun centis(ms: Long) = "%02d".format(ms / 10 % 100)
private fun full(ms: Long) = main(ms) + "." + centis(ms)

private data class LapRow(val number: Int, val lap: Long, val total: Long, val tag: Int?)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StopwatchScreen(contentPadding: PaddingValues) {
    val store = LocalContext.current.app.stopwatch
    val s by store.state.collectAsState()
    // Frame-synced clock while running so the centiseconds roll smoothly.
    val now by produceState(SystemClock.elapsedRealtime(), s.running) {
        value = SystemClock.elapsedRealtime()
        while (s.running) withFrameMillis { value = SystemClock.elapsedRealtime() }
    }
    val elapsed = s.elapsed(now)
    val c = MaterialTheme.colorScheme
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Recorded laps (newest first) only change on Lap or Reset, so they and their stats are built once, not every
    // frame; only the "Current" row ticks.
    val recorded = remember(s.laps) {
        val durations = s.lapDurations
        val fastest = s.fastestLap
        val slowest = s.slowestLap
        durations.indices.reversed().map { i ->
            LapRow(i + 1, durations[i], s.laps[i], when (i) { fastest -> R.string.fastest; slowest -> R.string.slowest; else -> null })
        }
    }
    val last = s.laps.lastOrNull() ?: 0
    val rows = if (s.laps.isNotEmpty() && elapsed > last) listOf(LapRow(s.laps.size + 1, elapsed - last, elapsed, R.string.current)) + recorded else recorded
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = LocalSnackbar.current
    var full by rememberSaveable { mutableStateOf(false) }
    var hideCs by remember { mutableStateOf(context.app.fullScreenHideHundredths) }
    if (full) {
        val toggle = { store.update { st, t -> if (st.running) st.stop(t) else st.start(t) } }
        FullScreenTime(
            // Hours only once there are any.
            groups = listOfNotNull(
                (elapsed / 3_600_000).takeIf { it > 0 }?.let { "%02d".format(it) },
                "%02d".format(elapsed / 60_000 % 60),
                "%02d".format(elapsed / 1000 % 60),
                centis(elapsed).takeUnless { hideCs },
            ),
            accentLast = !hideCs,
            running = s.running,
            spoken = main(elapsed),
            title = stringResource(R.string.stopwatch_full_screen),
            onExit = { full = false },
            onTap = toggle,
            tapLabel = stringResource(if (s.running) R.string.pause else R.string.start),
        ) {
            FullScreenOption(R.drawable.ic_hundredths, stringResource(R.string.hide_hundredths), hideCs) {
                hideCs = it
                context.app.fullScreenHideHundredths = it
            }
        }
    }
    // Landscape: face and buttons sit in their own pane beside the laps, the face smaller (252dp, as drawn).
    val landscape = isLandscape()
    val controls = @Composable {
        Controls(
            running = s.running,
            canReset = elapsed > 0,
            onSecondary = {
                if (s.running) {
                    store.update { st, t -> st.lap(t) }
                } else {
                    val before = store.state.value
                    store.update { st, _ -> st.reset() }
                    snackbar?.show(resources.getString(R.string.stopwatch_reset), resources.getString(R.string.undo)) {
                        store.update { _, _ -> before }
                    }
                }
            },
            onToggle = { store.update { st, t -> if (st.running) st.stop(t) else st.start(t) } },
            resumeLabel = if (elapsed > 0) stringResource(R.string.resume) else stringResource(R.string.start),
            height = if (landscape) 80.dp else 96.dp,
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
      val list = @Composable {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = top + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                val shareTitle = stringResource(R.string.share_laps)
                ScreenHeader(stringResource(R.string.tab_stopwatch_title), onFullScreen = { full = true }) {
                    IconButton(
                        {
                            // Built on tap, from recorded laps only (the running "Current" one isn't a lap yet).
                            val lapsText = recorded.reversed().joinToString("\n") { r -> resources.getString(R.string.lap_label, r.number) + "  " + full(r.lap) + "  (" + full(r.total) + ")" }
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, lapsText), shareTitle))
                        },
                        enabled = s.laps.isNotEmpty(),
                    ) { Icon(painterResource(R.drawable.ic_share), shareTitle) }
                }
            }
            if (!landscape) {
                item { Face(s.running, s.laps.size, elapsed, Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
                item { Box(Modifier.padding(top = 16.dp)) { controls() } }
            }
            item {
                SectionHeader(stringResource(R.string.laps), if (s.laps.isNotEmpty()) stringResource(R.string.laps_recorded, s.laps.size) else null)
            }
            if (rows.isEmpty()) item { EmptyState(stringResource(R.string.laps_hint)) }
            itemsIndexed(rows, key = { _, r -> r.number }) { i, r -> Box(Modifier.animateItem()) { LapItem(r, segmentShape(i, rows.size)) } }
        }
      }
        if (landscape) {
            TwoPane(contentPadding, hero = {
                Face(s.running, s.laps.size, elapsed, Modifier.fillMaxWidth(), small = true)
                Box(Modifier.padding(top = 12.dp)) { controls() }
            }) { list() }
        } else {
            list()
        }
    }
}

/** The stopwatch face: a cookie that spins while running (tertiaryContainer), with the time and a status line. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Face(running: Boolean, laps: Int, elapsed: Long, modifier: Modifier, small: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val face by animateColorAsState(if (running) c.onTertiaryContainer else c.onSurface, MaterialTheme.motionScheme.defaultEffectsSpec())
    val size = if (small) 252.dp else 320.dp
    Box(modifier, contentAlignment = Alignment.Center) {
        if (running && !LocalReduceMotion.current) {
            LoadingIndicator(Modifier.size(size), color = c.tertiaryContainer)
        } else {
            val bg = if (running) c.tertiaryContainer else c.surfaceContainerLow
            Box(Modifier.size(size).clip(MaterialShapes.Cookie9Sided.toShape()).background(bg))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = main(elapsed)
            },
        ) {
            Row {
                Text(
                    main(elapsed),
                    Modifier.alignByBaseline(),
                    color = face,
                    style = clockDigits(if (small) 72.sp else 88.sp, width = 48f).copy(lineHeight = if (small) 72.sp else 88.sp, letterSpacing = (-1.5).sp, fontWeight = FontWeight(820)),
                )
                Text(
                    "." + centis(elapsed),
                    Modifier.alignByBaseline().padding(start = 2.dp),
                    color = c.primary,
                    style = clockDigits(if (small) 30.sp else 36.sp, width = 60f).copy(fontWeight = FontWeight(750)),
                )
            }
            Text(
                when {
                    running -> stringResource(R.string.lap_label, laps + 1)
                    elapsed > 0 -> stringResource(R.string.sw_paused)
                    else -> stringResource(R.string.sw_ready)
                },
                Modifier.padding(top = 6.dp),
                color = face,
                style = AppText.hint,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Controls(running: Boolean, canReset: Boolean, onSecondary: () -> Unit, onToggle: () -> Unit, resumeLabel: String, height: Dp) {
    val c = MaterialTheme.colorScheme
    // As in the design's button group: the pressed button grows a little wider and its corners tighten to 16.
    val lapPress = remember { MutableInteractionSource() }
    val togglePress = remember { MutableInteractionSource() }
    val lapPressed by lapPress.collectIsPressedAsState()
    val togglePressed by togglePress.collectIsPressedAsState()
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val lapWeight by animateFloatAsState(if (lapPressed) 2.4f else 2f, spec)
    val toggleWeight by animateFloatAsState(if (togglePressed) 1.35f else 1f, spec)
    val pressed = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = onSecondary,
            shapes = ButtonShapes(shape = CircleShape, pressedShape = pressed),
            modifier = Modifier.weight(lapWeight).height(height),
            enabled = running || canReset,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.surfaceContainerLow, contentColor = c.onSurface),
            interactionSource = lapPress,
        ) {
            Icon(painterResource(if (running) R.drawable.ic_flag else R.drawable.ic_reset), null, Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(if (running) R.string.lap else R.string.reset),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight(750)),
            )
        }
        ToggleButton(
            checked = running,
            onCheckedChange = { onToggle() },
            modifier = Modifier.weight(toggleWeight).height(height),
            shapes = ToggleButtonShapes(shape = CircleShape, pressedShape = pressed, checkedShape = RoundedCornerShape(28.dp)),
            interactionSource = togglePress,
            colors = ToggleButtonDefaults.colors(
                containerColor = c.primary,
                contentColor = c.onPrimary,
                checkedContainerColor = c.error,
                checkedContentColor = c.onError,
            ),
        ) {
            Icon(
                painterResource(if (running) R.drawable.ic_pause else R.drawable.ic_play),
                if (running) stringResource(R.string.pause) else resumeLabel,
                Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun LapItem(r: LapRow, shape: androidx.compose.ui.graphics.Shape) {
    val c = MaterialTheme.colorScheme
    val (tagBg, tagFg) = when (r.tag) {
        R.string.fastest -> c.primaryContainer to c.onPrimaryContainer
        R.string.slowest -> c.errorContainer to c.onErrorContainer
        else -> c.tertiaryContainer to c.onTertiaryContainer
    }
    Surface(shape = shape, color = c.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.lap_label, r.number),
                Modifier.width(56.dp),
                style = clockDigits(15.sp).copy(fontWeight = FontWeight(700)),
                color = c.onSurfaceVariant,
            )
            Box(Modifier.weight(1f)) {
                if (r.tag != null) {
                    Text(
                        stringResource(r.tag),
                        Modifier.clip(CircleShape).background(tagBg).padding(horizontal = 10.dp, vertical = 4.dp),
                        color = tagFg,
                        style = MaterialTheme.typography.labelMediumEmphasized,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(full(r.lap), style = clockDigits(22.sp, width = 70f).copy(lineHeight = 26.sp, fontWeight = FontWeight(780)))
                Text(full(r.total), style = clockDigits(12.sp).copy(fontWeight = FontWeight(550)), color = c.onSurfaceVariant)
            }
        }
    }
}

