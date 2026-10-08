package com.nautesh.challengeclock.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// One variable file serves every weight; each Font entry pins its axes so weight matching uses the real axis.
@OptIn(ExperimentalTextApi::class)
private fun flexFamily(width: Float, opticalSize: Float? = null) = FontFamily(
    (1..10).map { step ->
        val weight = step * 100
        Font(
            "fonts/roboto_flex.ttf",
            fontAssets,
            FontWeight(weight),
            variationSettings = FontVariation.Settings(
                *listOfNotNull(FontVariation.weight(weight), FontVariation.width(width), opticalSize?.let { FontVariation.Setting("opsz", it) }).toTypedArray(),
            ),
        )
    },
)

val RobotoFlex = flexFamily(width = 100f)

// Tall, narrow digits for the big clock faces (Roboto Flex width axis runs 25–151), at default optical size.
val CondensedDigits = flexFamily(width = 42f)

val ClockDigitsStyle = TextStyle(
    fontFamily = CondensedDigits,
    fontWeight = FontWeight.ExtraBold,
    fontFeatureSettings = "tnum",
)

private val digitFamilies = HashMap<String, FontFamily>()

/**
 * Clock digits at [size] with Roboto Flex's optical size set to match, as browsers do automatically.
 * Android leaves opsz at 14, where the width axis barely narrows digits and lighter weights don't
 * contract, so the faces came out wide and unbolding an alarm didn't tighten them like the design.
 */
fun clockDigits(size: TextUnit, width: Float = 42f): TextStyle {
    val opsz = size.value.roundToInt().coerceIn(8, 144)
    val family = digitFamilies.getOrPut("$opsz/$width") { flexFamily(width, opsz.toFloat()) }
    return ClockDigitsStyle.copy(fontSize = size, fontFamily = family)
}

/** Roboto Flex text at any width axis value, optical size matched to [size] (e.g. the wide Stop label). */
fun flexText(size: TextUnit, width: Float, weight: Int): TextStyle {
    val opsz = size.value.roundToInt().coerceIn(8, 144)
    val family = digitFamilies.getOrPut("text/$opsz/$width") { flexFamily(width, opsz.toFloat()) }
    return TextStyle(fontFamily = family, fontSize = size, fontWeight = FontWeight(weight))
}

/** Tab titles as designed: Roboto Flex widened to 120, weight 780, optical size matched to 48sp. */
val ScreenTitleStyle by lazy {
    TextStyle(fontFamily = flexFamily(120f, 48f), fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight(780), letterSpacing = (-0.8).sp)
}

/**
 * The app's text roles: Material's type scale with the weights and sizes the design settled on. Use these rather
 * than copying a Material style with a one-off weight or size, so the same kind of text looks the same everywhere.
 * (Clock digits are separate: see [clockDigits].)
 */
object AppText {
    /** List section heading: "All alarms", "World", "Laps"; also the editor's top bar title. */
    val sectionTitle: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight(750))

    /** The count beside a section heading ("3 more", "4 cities"), in onSurfaceVariant. */
    val sectionCount: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight(600))

    /** Heading of a group of settings in a form ("Challenges", "Options"). */
    val formSection: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(750))

    /** First line of a list or settings row. */
    val rowTitle: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(650))

    /** Second line of a row, in onSurfaceVariant. */
    val rowDetail: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)

    /** Label of a tile (alarm, timer); tiles animate its weight with their on/off state. */
    val cardLabel: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight(600))

    /** Small second line on a tile (repeat days, "ends at"). */
    val cardDetail: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight(550))

    /** Label of a big full-width button (Save, Check, Snooze), and other lines that need the same weight. */
    val prominent: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight(750))

    /** Label of a menu pill or a floating action (Delete alarm). */
    val pillLabel: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(700))

    /** Label in a segmented or toggle button (days, AM/PM, snooze lengths), or a chip. */
    val choiceLabel: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight(700))

    /** A status or instruction line: challenge hints, "Alarm muted", the stopwatch state. */
    val hint: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight(700))

    /** Screen subtitle and date lines under a big clock. */
    val subtitle: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(550))

    /** All-caps caption under or over an input ("HOUR", "ANSWER"). */
    val fieldCaption: TextStyle @Composable @ReadOnlyComposable get() =
        MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight(650), letterSpacing = 0.4.sp)

    /** Large statement on a full-screen state: the ringing alarm's label, "Alarm dismissed". */
    val statement: TextStyle @Composable @ReadOnlyComposable get() =
        MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight(800))
}

private fun TextStyle.flex() = copy(fontFamily = RobotoFlex)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AppTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.flex(), displayMedium = displayMedium.flex(), displaySmall = displaySmall.flex(),
        headlineLarge = headlineLarge.flex(), headlineMedium = headlineMedium.flex(), headlineSmall = headlineSmall.flex(),
        titleLarge = titleLarge.flex(), titleMedium = titleMedium.flex(), titleSmall = titleSmall.flex(),
        bodyLarge = bodyLarge.flex(), bodyMedium = bodyMedium.flex(), bodySmall = bodySmall.flex(),
        labelLarge = labelLarge.flex(), labelMedium = labelMedium.flex(), labelSmall = labelSmall.flex(),
        displayLargeEmphasized = displayLargeEmphasized.flex(),
        displayMediumEmphasized = displayMediumEmphasized.flex(),
        displaySmallEmphasized = displaySmallEmphasized.flex(),
        headlineLargeEmphasized = headlineLargeEmphasized.flex(),
        headlineMediumEmphasized = headlineMediumEmphasized.flex(),
        headlineSmallEmphasized = headlineSmallEmphasized.flex(),
        titleLargeEmphasized = titleLargeEmphasized.flex(),
        titleMediumEmphasized = titleMediumEmphasized.flex(),
        titleSmallEmphasized = titleSmallEmphasized.flex(),
        bodyLargeEmphasized = bodyLargeEmphasized.flex(),
        bodyMediumEmphasized = bodyMediumEmphasized.flex(),
        bodySmallEmphasized = bodySmallEmphasized.flex(),
        labelLargeEmphasized = labelLargeEmphasized.flex(),
        labelMediumEmphasized = labelMediumEmphasized.flex(),
        labelSmallEmphasized = labelSmallEmphasized.flex(),
    )
}
