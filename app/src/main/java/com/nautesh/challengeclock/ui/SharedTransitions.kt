package com.nautesh.challengeclock.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/** Set by AppShell around the NavHost, and per destination, so screens can share elements across navigation. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** The "New alarm" pill in the Alarms + menu morphs into the editor's Save button. */
const val NewAlarmMorphKey = "new-alarm-morph"

/**
 * Morphs this element into whichever element uses the same [key]; a no-op outside AppShell.
 * [scope] defaults to the navigation transition; pass an AnimatedVisibility/AnimatedContent scope to morph
 * between two elements on the same screen.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedMorph(key: String, shape: Shape = RoundedCornerShape(28), scope: AnimatedVisibilityScope? = null): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val anim = scope ?: LocalNavAnimatedScope.current ?: return this
    return with(shared) {
        this@sharedMorph.sharedBounds(
            rememberSharedContentState(key),
            anim,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}
