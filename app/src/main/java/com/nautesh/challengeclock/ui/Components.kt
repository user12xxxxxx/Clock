package com.nautesh.challengeclock.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Longest label an alarm or timer takes. */
const val LABEL_MAX_LENGTH = 40

/**
 * The app's one text field: a filled pill with no underline or floating caption, as the design draws it. On the
 * page color (primaryContainer) and on sheets it takes surfaceContainerLow, like switched-off cards.
 */
@Composable
fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        placeholder = placeholder?.let { { Text(it, Modifier.fillMaxWidth(), textAlign = textStyle.textAlign) } },
        leadingIcon = leadingIcon,
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

/** Full-width-style primary action (Check, Start, Back to alarms): 64dp, pill that tightens when pressed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, @DrawableRes icon: Int? = null) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        enabled = enabled,
        shapes = ButtonShapes(shape = RoundedCornerShape(32.dp), pressedShape = RoundedCornerShape(16.dp)),
        contentPadding = PaddingValues(horizontal = 32.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(24.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = AppText.prominent)
    }
}

/** Title at the top of a bottom sheet. */
@Composable
fun SheetTitle(text: String) {
    Text(text, Modifier.padding(start = 8.dp, bottom = 16.dp), style = MaterialTheme.typography.headlineSmall)
}

/** "All alarms · 3 more": a list's heading with an optional count on the right. */
@Composable
fun SectionHeader(title: String, count: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, Modifier.weight(1f), style = AppText.sectionTitle)
        if (count != null) Text(count, style = AppText.sectionCount, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** What a list shows while it has nothing in it ("No alarms yet. Tap + to add one."). */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(24.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
