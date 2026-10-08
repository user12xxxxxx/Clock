package com.nautesh.challengeclock.ui

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.nautesh.challengeclock.R

/** Opens the system alarm-tone picker; "" stands for the system default alarm sound. */
@Composable
fun rememberSoundPicker(current: String, onPicked: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        onPicked(if (uri == null || uri == Settings.System.DEFAULT_ALARM_ALERT_URI) "" else uri.toString())
    }
    return {
        launcher.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI)
                .putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    if (current.isBlank()) Settings.System.DEFAULT_ALARM_ALERT_URI else Uri.parse(current),
                ),
        )
    }
}

fun soundTitle(context: Context, sound: String): String =
    if (sound.isBlank()) context.getString(R.string.sound_default)
    else runCatching { RingtoneManager.getRingtone(context, Uri.parse(sound))?.getTitle(context) }.getOrNull()
        ?: context.getString(R.string.sound_default)
