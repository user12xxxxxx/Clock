package com.nautesh.challengeclock.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.RingingActivity
import com.nautesh.challengeclock.alarm.RingingService
import com.nautesh.challengeclock.ui.alarms.AlarmsScreen
import com.nautesh.challengeclock.ui.alarms.EditAlarmScreen
import com.nautesh.challengeclock.ui.timers.TimerScreen
import com.nautesh.challengeclock.ui.clock.ClockScreen
import com.nautesh.challengeclock.ui.stopwatch.StopwatchScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

enum class Tab(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    Alarms("alarms", R.string.tab_alarm, R.drawable.ic_tab_alarm),
    Clock("clock", R.string.tab_clock, R.drawable.ic_tab_clock),
    Timer("timer", R.string.tab_timer, R.drawable.ic_tab_timer),
    Stopwatch("stopwatch", R.string.tab_stopwatch, R.drawable.ic_tab_stopwatch),
}

/**
 * The app-wide snackbar for confirmations and Undo. It lives above the NavHost, so a message posted as a screen
 * closes (the editor's Save or Delete) still shows on the screen underneath.
 */
class AppSnackbar(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, action: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            if (host.showSnackbar(message, action, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

val LocalSnackbar = staticCompositionLocalOf<AppSnackbar?> { null }

// Floating bar: 64dp tall + 16dp inset, so tab content keeps this much clear space at the bottom.
private val NavBarClearance = 64.dp + 16.dp * 2

// Landscape rail: 88dp wide + 12dp inset each side, so tab content starts this far in.
private val RailClearance = 88.dp + 12.dp * 2

/** Landscape swaps the bottom bar for a rail, and tab screens for two panes (a hero beside a scrolling list). */
@Composable
@ReadOnlyComposable
fun isLandscape(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppShell() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val currentTab = Tab.entries.firstOrNull { it.route == current?.destination?.route }

    val landscape = isLandscape()
    val dir = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val systemBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Landscape: content clears the rail on the start side and a side-mounted system bar or cutout on the end; the
    // extra 8dp at the bottom puts the + button 16dp off the edge, as the canvas draws it.
    val tabPadding = if (landscape) {
        PaddingValues(start = RailClearance + safe.calculateStartPadding(dir), end = safe.calculateEndPadding(dir), bottom = systemBottom + 8.dp)
    } else {
        PaddingValues(bottom = NavBarClearance + systemBottom)
    }

    // While an alarm rings, opening the app lands on its ringing screen. With notifications blocked there is no
    // full-screen alert or notification to tap, so this is the only way to reach Stop.
    val context = LocalContext.current
    val ringing by RingingService.ringingId.collectAsState()
    LifecycleResumeEffect(ringing) {
        ringing?.let { context.startActivity(RingingActivity.intent(context, it)) }
        onPauseOrDispose {}
    }

    val haze = rememberHazeState()
    val surface = MaterialTheme.colorScheme.primaryContainer // the page color behind the status bar
    // An open FAB menu dims its screen itself; this dims the nav bar and status strip to match (see LocalMenuScrim).
    val menuScrim = remember { mutableStateOf<(() -> Unit)?>(null) }
    val dim by animateFloatAsState(if (menuScrim.value != null) 1f else 0f, MaterialTheme.motionScheme.defaultEffectsSpec())
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
    // The ⋮ menu is a popup above the whole app, so here one full-window layer dims everything under it.
    val screenScrim = remember { mutableStateOf(false) }
    val snackHost = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    val snackbar = remember { AppSnackbar(snackHost, snackScope) }
    val select: (Tab) -> Unit = { tab ->
        // While a menu is open the bar is dimmed: a tap closes the menu, like tapping the dim layer.
        val close = menuScrim.value
        if (close != null) close() else nav.navigate(tab.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Shared-element scope so the + menu's "New alarm" pill can morph into the editor's Save button.
    // Page color behind the screens: during the fade-through both screens are briefly see-through, and the bare
    // window (white) used to flash between them.
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer)) {
    SharedTransitionLayout(Modifier.fillMaxSize().hazeSource(haze)) {
    CompositionLocalProvider(
        LocalSharedTransitionScope provides this, LocalMenuScrim provides menuScrim, LocalScreenScrim provides screenScrim, LocalSnackbar provides snackbar,
    ) {
        // Fade-through between screens: the old one fades out fast, the new one fades in while growing from 92%.
        // Reduce motion: a plain crossfade.
        val reduce = LocalReduceMotion.current
        val enter = if (reduce) fadeIn() else fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.92f)
        val exit = if (reduce) fadeOut() else fadeOut(tween(90))
        NavHost(
            nav, startDestination = Tab.Alarms.route, Modifier.fillMaxSize(),
            enterTransition = { enter }, exitTransition = { exit }, popEnterTransition = { enter }, popExitTransition = { exit },
        ) {
            composable(Tab.Alarms.route) {
                CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                    AlarmsScreen(tabPadding, onOpenAlarm = { id -> nav.navigate("alarm/$id") })
                }
            }
            composable(Tab.Clock.route) { ClockScreen(tabPadding) }
            composable(Tab.Timer.route) { TimerScreen(tabPadding) }
            composable(Tab.Stopwatch.route) { StopwatchScreen(tabPadding) }
            composable("alarm/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                    // Pop only while the editor is still on top: a second close (double tap, Back during a save)
                    // would otherwise pop Alarms too and leave an empty NavHost with no nav bar.
                    EditAlarmScreen(entry.arguments!!.getLong("id"), onClose = { if (nav.currentBackStackEntry == entry) nav.popBackStack() })
                }
            }
        }
    }
    }
        // Frosted strip behind the status bar so scrolled content doesn't collide with the clock and icons.
        // It runs 24dp past the bar and fades out over that stretch, so there's no hard bottom edge.
        val barHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            Modifier
                .fillMaxWidth()
                .height(barHeight + 24.dp)
                // Fades out while a menu is open: the screen's own dim layer already reaches under the status bar,
                // and dimming this blurred, tinted strip on top of it left a darker band. (Before hazeBlur, so it
                // wraps the blur's own drawing.)
                .graphicsLayer { alpha = 1f - dim }
                .hazeBlur(
                    HazeInput.Sources(haze),
                    HazeBlurStyle {
                        blurRadius(20.dp)
                        backgroundColor(surface)
                        colorEffects(listOf(HazeColorEffect.tint(surface.copy(alpha = 0.6f))))
                        mask(Brush.verticalGradient(0f to Color.Black, barHeight / (barHeight + 24.dp) to Color.Black, 1f to Color.Transparent))
                    },
                ),
        )
        if (currentTab != null && landscape) {
            FloatingNavRail(currentTab, dim, scrim, { tab -> select(tab) }, Modifier.align(Alignment.CenterStart))
        } else if (currentTab != null) {
            FloatingNavBar(
                selected = currentTab,
                dim = dim,
                scrim = scrim,
                onSelect = { tab -> select(tab) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        // Above the nav bar and the 80dp + button beside it.
        SnackbarHost(snackHost, Modifier.align(Alignment.BottomCenter).padding(tabPadding).padding(bottom = 88.dp))
        AnimatedVisibility(screenScrim.value, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(scrim))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingNavBar(selected: Tab, dim: Float, scrim: Color, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    ShortNavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp)
            .clip(CircleShape)
            .drawWithContent { drawContent(); if (dim > 0f) drawRect(scrim, alpha = dim) },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        windowInsets = WindowInsets(0),
    ) {
        // Tertiary-container bar: labels and icons in onTertiaryContainer; the selected tab is an onPrimary pill
        // with a primary icon.
        val c = MaterialTheme.colorScheme
        val colors = ShortNavigationBarItemDefaults.colors().copy(
            selectedIconColor = c.primary,
            selectedTextColorTopIconPosition = c.onTertiaryContainer,
            selectedTextColorStartIconPosition = c.onTertiaryContainer,
            selectedIndicatorColor = c.onPrimary,
            unselectedIconColor = c.onTertiaryContainer,
            unselectedTextColor = c.onTertiaryContainer,
        )
        Tab.entries.forEach { tab ->
            ShortNavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                colors = colors,
            )
        }
    }
}

/** Landscape: the same bar as a floating 88dp rail down the start edge, its tabs centered vertically. */
@Composable
private fun FloatingNavRail(selected: Tab, dim: Float, scrim: Color, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    NavigationRail(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .padding(12.dp)
            .width(88.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(32.dp))
            .drawWithContent { drawContent(); if (dim > 0f) drawRect(scrim, alpha = dim) },
        containerColor = c.tertiaryContainer,
        windowInsets = WindowInsets(0),
    ) {
        val colors = NavigationRailItemDefaults.colors(
            selectedIconColor = c.primary,
            selectedTextColor = c.onTertiaryContainer,
            indicatorColor = c.onPrimary,
            unselectedIconColor = c.onTertiaryContainer,
            unselectedTextColor = c.onTertiaryContainer,
        )
        Spacer(Modifier.weight(1f))
        Tab.entries.forEachIndexed { i, tab ->
            if (i > 0) Spacer(Modifier.height(16.dp))
            NavigationRailItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                colors = colors,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/**
 * Landscape tab layout as the canvas draws it: [hero] in a 360dp pane, centered top to bottom (it scrolls if the
 * screen is too short), beside [list], which fills the rest and scrolls on its own. [contentPadding] carries the
 * rail's clearance on the start side.
 */
@Composable
fun TwoPane(contentPadding: PaddingValues, hero: @Composable ColumnScope.() -> Unit, list: @Composable BoxScope.() -> Unit) {
    val dir = LocalLayoutDirection.current
    Row(Modifier.fillMaxSize().padding(start = contentPadding.calculateStartPadding(dir), end = contentPadding.calculateEndPadding(dir))) {
        Box(Modifier.width(360.dp).fillMaxHeight().windowInsetsPadding(WindowInsets.statusBars), contentAlignment = Alignment.Center) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp), content = hero)
        }
        Box(Modifier.weight(1f).padding(start = 8.dp), content = list)
    }
}
