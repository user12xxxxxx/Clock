package com.nautesh.challengeclock.ui

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Hero cards: rounded everywhere except a tighter top-end corner, as hand-tuned on the canvas.
val HeroCardShape = RoundedCornerShape(topStart = 32.dp, topEnd = 12.dp, bottomEnd = 32.dp, bottomStart = 32.dp)

/** True when the user turned on "Remove animations": no overshoot, no morphing shapes, no moving waves. */
val LocalReduceMotion = staticCompositionLocalOf { false }

// Colors follow Material You (wallpaper-based); minSdk 34 guarantees dynamic color is available.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChallengeClockTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    // ponytail: read once per activity start; toggling the setting while the app is open takes effect on next launch.
    val reduceMotion = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialExpressiveTheme(
        // Roles as the canvas's Clock Palette assigns them: page and sheets primaryContainer; anything switched off
        // (cards, fields, rows, days) surfaceContainerLow; switched on or running primary; floating chrome (nav,
        // + button, its menus, Save) and a running stopwatch tertiaryContainer; a finished timer or Stop error.
        // errorContainer is only for small marks (chips, decorative shapes).
        colorScheme = scheme,
        motionScheme = if (reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
        typography = AppTypography,
        shapes = Shapes(), // defaults already carry the Expressive steps (largeIncreased 20, extraLargeIncreased 32, extraExtraLarge 48)
    ) {
        CompositionLocalProvider(LocalReduceMotion provides reduceMotion, content = content)
    }
}
