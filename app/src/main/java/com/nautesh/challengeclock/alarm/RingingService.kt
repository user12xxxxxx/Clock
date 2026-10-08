package com.nautesh.challengeclock.alarm

import android.app.Notification
import android.text.format.DateFormat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.nautesh.challengeclock.MainActivity
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.RingingActivity
import com.nautesh.challengeclock.app
import com.nautesh.challengeclock.data.Alarm
import com.nautesh.challengeclock.data.challengeTypes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

// ponytail: fixed auto-silence after RING_TIMEOUT_MS; make it a per-alarm setting if users ask
class RingingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var alarm: Alarm? = null
    private var muteJob: Job? = null
    // Safety net: nothing rings forever, even if no screen or notification is reachable to stop it.
    private var timeoutJob: Job? = null
    private var snoozes = 0
    private var ringJob: Job? = null
    private var alarmNotification: Notification? = null
    // Alarms (id, snoozes used) that fired while another rang, and a timer that finished meanwhile. Once the
    // ringing alarm is stopped or snoozed, the queued alarms ring in turn, then the timer.
    private val pendingAlarms = ArrayDeque<Pair<Long, Int>>()
    private var pendingTimer: Long? = null
    private val vibrator: Vibrator by lazy { getSystemService(VibratorManager::class.java).defaultVibrator }
    private val alarmAudio = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getLongExtra(AlarmReceiver.EXTRA_ID, -1) ?: -1
        when (intent?.action) {
            ACTION_STOP -> stop()
            ACTION_MUTE -> mute(intent.getIntExtra(EXTRA_SECONDS, 0))
            ACTION_TIMER -> ringTimer(id)
            ACTION_STOP_TIMER -> if (_ringingTimerId.value == id) stop()
            ACTION_SNOOZE -> if (_canSnooze.value) scope.launch {
                app.alarms.snooze(id, snoozes + 1)
                stop()
            }
            else -> {
                val count = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, 0) ?: 0
                val current = _ringingId.value
                if (current == null || current == id) {
                    ring(id, count)
                } else {
                    // Another alarm is ringing: this one waits its turn instead of silently replacing it.
                    pendingAlarms.addLast(id to count)
                    alarmNotification?.let { startForeground(NOTIFICATION_ID, it, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun ring(id: Long, snoozesUsed: Int) {
        stopSound()
        ringJob?.cancel()
        _ringingTimerId.value?.let { pendingTimer = it } // an alarm takes over a ringing timer; it comes back after
        _ringingTimerId.value = null
        alarm = null
        snoozes = snoozesUsed
        _canSnooze.value = true
        val first = notification(id, getString(R.string.alarm), null, canStop = false, canSnooze = true)
        alarmNotification = first
        startForeground(NOTIFICATION_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        _ringToken.value = System.currentTimeMillis()
        _ringingId.value = id
        startTimeout()
        ringJob = scope.launch {
            val loaded = app.alarms.get(id)
            playSound(loaded?.sound.orEmpty())
            if (loaded == null) return@launch
            alarm = loaded
            _canSnooze.value = loaded.snoozeLimit == 0 || snoozes < loaded.snoozeLimit
            if (loaded.vibrate) vibrate()
            val title = loaded.label.ifBlank { getString(R.string.alarm) }
            // With challenges, stopping has to go through the ringing screen.
            val full = notification(
                id, title, DateFormat.getTimeFormat(this@RingingService).format(Date.from(LocalTime.of(loaded.hour, loaded.minute).atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant())),
                canStop = loaded.challengeTypes.isEmpty(), canSnooze = _canSnooze.value,
            )
            alarmNotification = full
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, full)
        }
    }

    // Finished timer: sound + vibration with a plain heads-up notification and a Stop action.
    private fun ringTimer(id: Long) {
        if (_ringingId.value != null) { // never interrupt a ringing alarm; queue the timer instead
            pendingTimer = id
            // startForegroundService() still expects startForeground(): re-post the alarm's own notification. Kept
            // in a field, not looked up, since the user may have swiped the posted one away (allowed on Android 14+).
            alarmNotification?.let { startForeground(NOTIFICATION_ID, it, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) }
            return
        }
        stopSound()
        startForeground(NOTIFICATION_ID, timerNotification(id, getString(R.string.times_up)), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        _ringingTimerId.value = id
        playSound(app.timerSound)
        vibrate()
        startTimeout()
        scope.launch {
            val label = app.timers.get(id)?.label?.ifBlank { null } ?: getString(R.string.tab_timer)
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, timerNotification(id, label))
        }
    }

    private fun timerNotification(id: Long, text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ensureChannel())
            .setSmallIcon(R.drawable.ic_tab_timer)
            .setContentTitle(getString(R.string.times_up))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop), action(ACTION_STOP, id))
            .build()
    }

    // A picked tone that has since been deleted falls back to the system alarm sound.
    private fun playSound(sound: String) {
        val fallback = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        // ponytail: no fallback tone if the device has no ringtone at all; vibration still runs
        player = listOfNotNull(sound.ifBlank { null }?.let(Uri::parse), fallback).firstNotNullOfOrNull { uri ->
            val candidate = MediaPlayer()
            runCatching {
                candidate.apply {
                    setAudioAttributes(alarmAudio)
                    setDataSource(this@RingingService, uri)
                    isLooping = true
                    setWakeMode(this@RingingService, PowerManager.PARTIAL_WAKE_LOCK)
                    prepare()
                    start()
                }
            }.onFailure { candidate.release() }.getOrNull()
        }
    }

    private fun vibrate() = vibrator.vibrate(
        VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0),
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
    )

    // Silence for a while once a challenge starts; calling again restarts the countdown.
    private fun mute(seconds: Int) {
        if (seconds <= 0) return
        muteJob?.cancel()
        player?.takeIf { it.isPlaying }?.pause()
        vibrator.cancel()
        _muted.value = true
        muteJob = scope.launch {
            delay(seconds * 1000L)
            _muted.value = false
            player?.start()
            if (alarm?.vibrate == true) vibrate()
        }
    }

    private fun startTimeout() {
        timeoutJob = scope.launch {
            delay(RING_TIMEOUT_MS)
            stop()
        }
    }

    private fun stopSound() {
        timeoutJob?.cancel()
        muteJob?.cancel()
        _muted.value = false
        player?.release()
        player = null
        vibrator.cancel()
    }

    private fun stop() {
        stopSound()
        _ringingId.value = null
        _ringingTimerId.value = null
        pendingAlarms.removeFirstOrNull()?.let { (id, count) -> return ring(id, count) }
        val pending = pendingTimer ?: return finish()
        pendingTimer = null
        // Ring the queued timer now, unless it was reset or removed meanwhile.
        scope.launch { if (app.timers.get(pending)?.done == true) ringTimer(pending) else finish() }
    }

    private fun finish() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        _ringingId.value = null
        _ringingTimerId.value = null
        scope.cancel()
    }

    private fun ensureChannel(): String {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_ringing), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // the service plays the sound itself
                enableVibration(false)
            },
        )
        return CHANNEL
    }

    private fun notification(id: Long, title: String, text: String?, canStop: Boolean, canSnooze: Boolean): Notification {
        ensureChannel()
        val fullScreen = PendingIntent.getActivity(
            this, 0,
            RingingActivity.intent(this, id).addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
        if (canSnooze) builder.addAction(0, getString(R.string.snooze), action(ACTION_SNOOZE, id))
        if (canStop) builder.addAction(0, getString(R.string.stop), action(ACTION_STOP, id))
        return builder.build()
    }

    private fun action(action: String, id: Long) = PendingIntent.getService(
        this, action.hashCode(), intent(this, id, action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ACTION_STOP = "com.nautesh.challengeclock.STOP"
        const val ACTION_SNOOZE = "com.nautesh.challengeclock.SNOOZE"
        const val ACTION_MUTE = "com.nautesh.challengeclock.MUTE"
        const val ACTION_TIMER = "com.nautesh.challengeclock.TIMER"
        const val ACTION_STOP_TIMER = "com.nautesh.challengeclock.STOP_TIMER"
        const val EXTRA_SECONDS = "seconds"
        private const val CHANNEL = "ringing"
        private const val NOTIFICATION_ID = 1
        private const val RING_TIMEOUT_MS = 15 * 60_000L

        private val _ringingId = MutableStateFlow<Long?>(null)
        val ringingId: StateFlow<Long?> = _ringingId

        // Wall-clock start of the latest alarm ring. Unique per ring, even across process restarts, so the
        // ringing screen can tell a new ring of the same alarm from the one it already showed.
        private val _ringToken = MutableStateFlow(0L)
        val ringToken: StateFlow<Long> = _ringToken

        // False once the ringing alarm has used up its snooze limit.
        private val _canSnooze = MutableStateFlow(true)
        val canSnooze: StateFlow<Boolean> = _canSnooze

        // True while a challenge has the alarm muted.
        private val _muted = MutableStateFlow(false)
        val muted: StateFlow<Boolean> = _muted

        private val _ringingTimerId = MutableStateFlow<Long?>(null)
        val ringingTimerId: StateFlow<Long?> = _ringingTimerId

        fun intent(context: Context, id: Long, action: String? = null): Intent =
            Intent(context, RingingService::class.java).setAction(action).putExtra(AlarmReceiver.EXTRA_ID, id)
    }
}
