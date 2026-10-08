package com.nautesh.challengeclock.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.nautesh.challengeclock.R

/**
 * Set by AppShell. While a FAB menu is open it holds that menu's close action, so AppShell can dim the nav bar
 * and the status-bar strip too (they're drawn above the screen, out of reach of the menu's own dim layer).
 */
val LocalMenuScrim = staticCompositionLocalOf<MutableState<(() -> Unit)?>?> { null }

/** Set by AppShell. True while a popup menu (the ⋮ menu) is open; AppShell then dims the whole app behind it. */
val LocalScreenScrim = staticCompositionLocalOf<MutableState<Boolean>?> { null }

/** [morphKey]: this pill morphs into the element with the same key on the screen it opens (see sharedMorph). */
data class FabAction(@param:DrawableRes val icon: Int, val label: String, val morphKey: String? = null, val onClick: () -> Unit)

/**
 * The create menu as the design draws it: a flat 80dp FAB (no shadow) that morphs from a rounded
 * square to a rounder primary button and opens a stack of pills above it. [actions] run top to bottom.
 */
@Composable
fun FabMenu(actions: List<FabAction>, modifier: Modifier = Modifier) {
    // Plain remember: the menu is closed again when you come back to the screen, and a pill that morphs into
    // the next screen can stay put while that transition runs.
    var expanded by remember { mutableStateOf(false) }
    BackHandler(expanded) { expanded = false }
    val menuScrim = LocalMenuScrim.current
    DisposableEffect(expanded) {
        if (expanded) menuScrim?.value = { expanded = false }
        onDispose { menuScrim?.value = null }
    }
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val progress by animateFloatAsState(if (expanded) 1f else 0f, motion.defaultSpatialSpec())
    // Closed: tertiaryContainer, like the nav bar beside it; open: primary so the state change still shows.
    val container by animateColorAsState(if (expanded) c.primary else c.tertiaryContainer, motion.defaultEffectsSpec())
    val content by animateColorAsState(if (expanded) c.onPrimary else c.onTertiaryContainer, motion.defaultEffectsSpec())
    val stateLabel = stringResource(if (expanded) R.string.menu_open else R.string.menu_closed)

    // Dims the screen behind the open menu; tapping it closes the menu. The menu's own [modifier] (usually an
    // align() from the caller's Box) applies to the Column below, which this Box now parents.
    Box(Modifier.fillMaxSize()) {
    AnimatedVisibility(expanded, enter = fadeIn(motion.defaultEffectsSpec()), exit = fadeOut(motion.defaultEffectsSpec())) {
        val closeLabel = stringResource(R.string.close)
        Box(
            Modifier
                .fillMaxSize()
                .background(c.scrim.copy(alpha = 0.32f))
                .clickable(interactionSource = null, indication = null, onClickLabel = closeLabel) { expanded = false },
        )
    }
    // The pills scroll if they still don't fit (large font sizes); the button stays put.
    val pills = @Composable { pillsModifier: Modifier, items: List<FabAction> ->
      Column(pillsModifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { action ->
            AnimatedVisibility(
                expanded,
                enter = fadeIn(motion.defaultEffectsSpec()) +
                    scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f, transformOrigin = TransformOrigin(1f, 0.5f)),
                exit = fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastEffectsSpec(), targetScale = 0.6f, transformOrigin = TransformOrigin(1f, 0.5f)),
            ) {
                MenuPill(
                    painterResource(action.icon),
                    action.label,
                    if (action.morphKey != null) Modifier.sharedMorph(action.morphKey) else Modifier,
                ) {
                    if (action.morphKey == null) expanded = false
                    action.onClick()
                }
            }
        }
      }
    }
    val fab = @Composable {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(lerp(24.dp, 40.dp, progress)),
            color = container,
            contentColor = content,
            modifier = Modifier.size(80.dp).semantics { stateDescription = stateLabel },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.open_menu), Modifier.size(32.dp).rotate(progress * 135f))
            }
        }
    }
    // 8dp at the bottom leaves the design's 24px gap above the nav bar (the screen already clears the bar by 16dp).
    // Clear of the status bar at the top, so the first pill never sits under its icons.
    val edges = modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
    if (isLandscape()) {
        // Too short to stack five pills above an 80dp button: up to three stay one stack; more wrap into two columns
        // above it, the first half in the column nearest the edge.
        Column(edges, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                actions.chunked(maxOf(3, (actions.size + 1) / 2)).reversed().forEach { pills(Modifier, it) }
            }
            fab()
        }
    } else {
        Column(edges, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) { pills(Modifier.weight(1f, fill = false), actions); fab() }
    }
    }
}

/** A 56dp pill: the create and overflow menus' items, and the editor's Delete (in errorContainer). */
@Composable
internal fun MenuPill(
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiaryContainer,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = color,
        contentColor = contentColorFor(color),
    ) {
        Row(Modifier.height(56.dp).padding(start = 18.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(24.dp))
            Spacer(Modifier.size(12.dp))
            Text(label, style = AppText.pillLabel)
        }
    }
}
