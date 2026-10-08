package com.nautesh.challengeclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Visual check for step 2: palette, type scale and shapes in one scroll.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeShowcase() {
    val c = MaterialTheme.colorScheme
    val t = MaterialTheme.typography
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Palette", style = t.titleMediumEmphasized)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "primary" to c.primary, "primaryCont" to c.primaryContainer, "secondaryCont" to c.secondaryContainer,
                    "tertiary" to c.tertiary, "tertiaryCont" to c.tertiaryContainer, "low" to c.surfaceContainerLow,
                    "container" to c.surfaceContainer, "high" to c.surfaceContainerHigh, "highest" to c.surfaceContainerHighest,
                ).forEach { (name, color) -> Swatch(name, color) }
            }

            Text("Type", style = t.titleMediumEmphasized)
            Text("Alarms", style = t.displayMediumEmphasized)
            Text("Headline large", style = t.headlineLarge)
            Text("Title medium emphasized", style = t.titleMediumEmphasized)
            Text("Body large: next alarm in 7 h 21 min", style = t.bodyLarge)
            Text("LABEL LARGE", style = t.labelLarge)

            Text("Shapes", style = t.titleMediumEmphasized)
            Column(
                Modifier.fillMaxWidth().clip(HeroCardShape).background(c.primary).padding(20.dp),
            ) {
                Text("06:30", style = clockDigits(112.sp).copy(lineHeight = 100.sp), color = c.onPrimary)
                Text("Weekdays", style = t.titleMedium, color = c.onPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ShapeBlob(MaterialShapes.Cookie9Sided.toShape(), c.primary)
                ShapeBlob(MaterialShapes.Sunny.toShape(), c.tertiaryContainer)
                ShapeBlob(MaterialShapes.Clover8Leaf.toShape(), c.secondaryContainer)
                ShapeBlob(MaterialShapes.Cookie4Sided.toShape(), c.primaryContainer)
            }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.large).background(color))
        Text(name, style = TextStyle(fontSize = 10.sp))
    }
}

@Composable
private fun ShapeBlob(shape: Shape, color: Color) = Box(Modifier.size(72.dp).clip(shape).background(color))

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ThemeShowcasePreview() = ChallengeClockTheme { ThemeShowcase() }

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ThemeShowcaseDarkPreview() = ChallengeClockTheme(darkTheme = true) { ThemeShowcase() }
