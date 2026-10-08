package com.nautesh.challengeclock.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nautesh.challengeclock.R

/**
 * Tab header: action buttons and the overflow menu top-right, then the big title and an optional subtitle.
 * [onFullScreen]: adds "Full screen" to the top of the overflow menu (Clock and Stopwatch).
 */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onFullScreen: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    // Landscape: a smaller title shares one row with the buttons, so the list keeps the short screen's height.
    val landscape = isLandscape()
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            if (landscape) Text(title, Modifier.weight(1f).padding(start = 8.dp), style = ScreenTitleStyle.copy(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.6).sp))
            actions()
            OverflowMenu(onFullScreen)
        }
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = if (landscape) 0.dp else 4.dp, bottom = 8.dp)) {
            if (!landscape) Text(title, style = ScreenTitleStyle)
            if (subtitle != null) {
                Text(
                    subtitle,
                    Modifier.padding(top = 6.dp),
                    style = AppText.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Options open as a stack of pills like the create (FAB) menu, rather than a plain dropdown.
@Composable
private fun OverflowMenu(onFullScreen: (() -> Unit)?) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    // OEM builds can drop a settings page; a missing one shouldn't crash the app.
    fun launch(intent: Intent) = runCatching { context.startActivity(intent) }
    val items = listOfNotNull(
        onFullScreen?.let { Triple(R.drawable.ic_fullscreen, R.string.full_screen, it) },
        Triple(R.drawable.ic_time, R.string.date_time_settings) { launch(Intent(Settings.ACTION_DATE_SETTINGS)) },
        Triple(R.drawable.ic_bell, R.string.notification_settings) {
            launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        },
        Triple(R.drawable.ic_info, R.string.about_app) { about = true },
    )
    if (about) AboutSheet { about = false }
    // The menu is its own popup window, above the app, so the app can be dimmed whole underneath it.
    val screenScrim = LocalScreenScrim.current
    DisposableEffect(open) {
        screenScrim?.value = open
        onDispose { screenScrim?.value = false }
    }
    Box {
        IconButton({ open = true }) { Icon(painterResource(R.drawable.ic_more), stringResource(R.string.more_options)) }
        if (open) {
            val shown = remember { MutableTransitionState(false).apply { targetState = true } }
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(androidx.compose.ui.platform.LocalDensity.current) { 52.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEachIndexed { i, (icon, label, action) ->
                        AnimatedVisibility(
                            shown,
                            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                                scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f, transformOrigin = TransformOrigin(1f, 0f)),
                        ) {
                            MenuPill(painterResource(icon), stringResource(label)) {
                                open = false
                                action()
                            }
                        }
                    }
                }
            }
        }
    }
}

