package com.nautesh.challengeclock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Big two-digit number box used for typed times (alarm time, custom timer). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeField(
    value: String,
    onValue: (String) -> Unit,
    onDone: () -> Unit,
    label: String,
    width: Dp = 112.dp,
    fontSize: TextUnit = 64.sp,
    digitWidth: Float = 42f,
    digitWeight: Int = 800,
) {
    var focused by remember { mutableStateOf(false) }
    // Treat every edit as "digits appended" or "one deleted" against our own value, since the keyboard
    // can hold a stale copy of the text. The first digits after focusing replace the number.
    var fresh by remember { mutableStateOf(false) }
    val field = TextFieldValue(value, selection = TextRange(value.length))
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (focused) c.primaryContainer else c.surfaceContainerLow, MaterialTheme.motionScheme.defaultEffectsSpec())
    val fg = if (focused) c.onPrimaryContainer else c.onSurface
    val radius by animateDpAsState(if (focused) 20.dp else 28.dp, MaterialTheme.motionScheme.fastSpatialSpec())
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = field,
            onValueChange = { edit ->
                val added = edit.text.length - value.length
                when {
                    added > 0 -> onValue((if (fresh) "" else value) + edit.text.takeLast(added))
                    added < 0 -> onValue(value.dropLast(1))
                }
                fresh = false
            },
            singleLine = true,
            textStyle = clockDigits(fontSize, digitWidth).copy(color = fg, textAlign = TextAlign.Center, fontWeight = FontWeight(digitWeight)),
            cursorBrush = SolidColor(c.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier
                .size(width = width, height = 96.dp)
                .semantics { contentDescription = label }
                .onFocusChanged { state ->
                    if (!focused && state.isFocused) fresh = true
                    if (focused && !state.isFocused) onDone()
                    focused = state.isFocused
                },
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxSize().background(bg, RoundedCornerShape(radius)),
                    contentAlignment = Alignment.Center,
                ) { inner() }
            },
        )
        Text(label, Modifier.padding(top = 6.dp), style = AppText.fieldCaption, color = c.onSurfaceVariant)
    }
}
