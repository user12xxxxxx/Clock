package com.nautesh.challengeclock.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.app
import kotlinx.coroutines.delay

/**
 * Only the time, as big as the screen allows: digit groups stacked (side by side in landscape), the last one in the
 * accent color when [accentLast]. System bars are hidden and the screen stays on. The corner controls fade after
 * 3 s; any touch brings them back. Back or the corner button calls [onExit].
 *
 * [running]: tertiaryContainer page (a clock always runs), else primaryContainer. [onTap]: tapping the time runs it
 * (the stopwatch's start/stop), announced as [tapLabel]. [options]: extra toggles beside True black.
 */
@Composable
fun FullScreenTime(
    groups: List<String>,
    accentLast: Boolean,
    running: Boolean,
    spoken: String,
    title: String,
    onExit: () -> Unit,
    meridiem: String? = null,
    onTap: (() -> Unit)? = null,
    tapLabel: String? = null,
    options: @Composable RowScope.() -> Unit = {},
) {
    Dialog(onExit, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setDimAmount(0f)
                WindowCompat.getInsetsController(window, view).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }

        val app = LocalContext.current.app
        var black by remember { mutableStateOf(app.fullScreenBlack) }
        val c = MaterialTheme.colorScheme
        // True black is the same in light and dark (for OLED screens): text in the dark scheme's onSurface and primary.
        val dark = dynamicDarkColorScheme(LocalContext.current)
        val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
        val bg by animateColorAsState(if (black) Color.Black else if (running) c.tertiaryContainer else c.primaryContainer, effects)
        val fg by animateColorAsState(if (black) dark.onSurface else if (running) c.onTertiaryContainer else c.onPrimaryContainer, effects)
        val accent = if (black) dark.primary else c.primary

        // Each touch bumps [wake]; the controls show and fade out 3 s after the last one.
        var wake by remember { mutableIntStateOf(0) }
        var chrome by remember { mutableStateOf(true) }
        LaunchedEffect(wake) {
            chrome = true
            delay(3000)
            chrome = false
        }

        CompositionLocalProvider(LocalContentColor provides fg) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(bg)
                    .semantics { paneTitle = title }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                wake++
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val landscape = maxWidth > maxHeight
                // As drawn: 200sp, shrunk to fit. ponytail: a two-digit group at width 48 is taken as ~0.9em wide;
                // measure the text instead if a locale's digits run wider.
                val gap = 28.dp
                val fit = run {
                    val n = groups.size
                    if (landscape) minOf((maxWidth - 48.dp - gap * (n - 1) - if (meridiem != null) 90.dp else 0.dp) / (0.9f * n), (maxHeight - 48.dp) / 0.88f)
                    else minOf((maxWidth - 48.dp) / 0.9f, (maxHeight - 48.dp - if (meridiem != null) 56.dp else 0.dp) / (0.88f * n))
                }
                // In sp, so a large font scale doesn't push the digits off the screen.
                val size = minOf(with(LocalDensity.current) { fit.toSp() }.value, 200f).sp
                val style = clockDigits(size, width = 48f).copy(lineHeight = size * 0.88f, letterSpacing = (-4).sp * (size.value / 200f), fontWeight = FontWeight(820))

                val tap = if (onTap != null) Modifier.clip(RoundedCornerShape(48.dp)).clickable(onClickLabel = tapLabel, onClick = onTap) else Modifier
                val face = Modifier.then(tap).padding(24.dp).semantics(mergeDescendants = true) { contentDescription = spoken }
                val digits = @Composable {
                    groups.forEachIndexed { i, g ->
                        Text(g, style = style, color = if (accentLast && i == groups.lastIndex) accent else fg)
                    }
                }
                val mer = @Composable { if (meridiem != null) Text(meridiem, style = clockDigits(40.sp).copy(fontWeight = FontWeight(750), letterSpacing = 1.sp)) }
                if (landscape) {
                    Row(face, horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) { digits(); mer() }
                } else {
                    Column(face, horizontalAlignment = Alignment.CenterHorizontally) {
                        digits()
                        if (meridiem != null) Column(Modifier.padding(top = 16.dp)) { mer() }
                    }
                }

                AnimatedVisibility(chrome, Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp), enter = fadeIn(), exit = fadeOut()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        FullScreenOption(R.drawable.ic_contrast, stringResource(R.string.true_black), black) {
                            black = it
                            app.fullScreenBlack = it
                        }
                        options()
                        IconButton(onExit) { Icon(painterResource(R.drawable.ic_fullscreen_exit), stringResource(R.string.exit_full_screen)) }
                    }
                }
            }
        }
    }
}

/** A toggle in the full-screen corner: a faint pill of the text color behind it while on. */
@Composable
fun FullScreenOption(@DrawableRes icon: Int, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val fg = LocalContentColor.current
    IconToggleButton(
        checked, onChange,
        colors = IconButtonDefaults.iconToggleButtonColors(contentColor = fg, checkedContentColor = fg, checkedContainerColor = fg.copy(alpha = 0.16f)),
    ) { Icon(painterResource(icon), label, Modifier.size(22.dp)) }
}
