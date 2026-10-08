package com.nautesh.challengeclock.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nautesh.challengeclock.R

// Empty until the repo is public; the source row is hidden until then.
private const val GITHUB_URL = ""

/** "About app" from the overflow menu: version and source link, laid out like StopIt's About page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.primaryContainer) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            // Titled like every other sheet; it closes by swipe, scrim tap or Back, as they do.
            SheetTitle(stringResource(R.string.about_app))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val hasSource = GITHUB_URL.isNotEmpty()
                InfoRow(
                    R.drawable.ic_info,
                    stringResource(R.string.version),
                    "${stringResource(R.string.app_name)} $version",
                    RoundedCornerShape(24.dp, 24.dp, if (hasSource) 6.dp else 24.dp, if (hasSource) 6.dp else 24.dp),
                )
                if (hasSource) {
                    InfoRow(
                        R.drawable.ic_github,
                        stringResource(R.string.source_code),
                        GITHUB_URL,
                        RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp),
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(@DrawableRes icon: Int, title: String, detail: String, shape: Shape, onClick: (() -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    val content: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = c.onSurfaceVariant)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
    }
    if (onClick != null) Surface(onClick = onClick, shape = shape, color = c.surfaceContainer, modifier = Modifier.fillMaxWidth(), content = content)
    else Surface(shape = shape, color = c.surfaceContainer, modifier = Modifier.fillMaxWidth(), content = content)
}
