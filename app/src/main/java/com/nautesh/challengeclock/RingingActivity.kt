package com.nautesh.challengeclock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.nautesh.challengeclock.alarm.AlarmReceiver
import com.nautesh.challengeclock.alarm.RingingService
import com.nautesh.challengeclock.data.Alarm
import com.nautesh.challengeclock.data.ChallengeType
import com.nautesh.challengeclock.data.toCsv
import com.nautesh.challengeclock.data.challengePlan
import com.nautesh.challengeclock.ui.ChallengeClockTheme
import com.nautesh.challengeclock.ui.ringing.AlarmDismissed
import com.nautesh.challengeclock.ui.ringing.RingingScreen

class RingingActivity : ComponentActivity() {
    private var lastMute = 0L
    // A second alarm firing re-delivers this singleTask activity with a new id.
    private val alarmId = mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        alarmId.longValue = intent.getLongExtra(AlarmReceiver.EXTRA_ID, -1)
        setContent {
            val ringing by RingingService.ringingId.collectAsState()
            // Follow whatever the service rings: a queued alarm takes over here once the current one is stopped.
            val id = ringing ?: alarmId.longValue
            SideEffect { alarmId.longValue = id } // so the id outlives the ring ("Alarm dismissed" keeps its key)
            // Keyed per ring, not just per alarm: a screen left on "Alarm dismissed" (or restored with it after
            // process death) must start fresh when the same alarm rings again, yet keep its challenge progress
            // when the same ring is merely reopened from the notification or the app.
            val ring by RingingService.ringToken.collectAsState()
            val muted by RingingService.muted.collectAsState()
            ChallengeClockTheme { key(id, ring) {
                // Stopped here: show "Alarm dismissed" instead of vanishing. Stopped elsewhere (notification, snooze): close.
                var dismissed by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(ringing, dismissed) { if (ringing == null && !dismissed) finish() }
                var loaded by remember { mutableStateOf(false) }
                val alarm by produceState<Alarm?>(null, id) { value = app.alarms.get(id); loaded = true }
                // Saved as ids so rotating mid-challenge keeps the same shuffled order; null until loaded.
                var planIds by rememberSaveable { mutableStateOf<String?>(null) }
                LaunchedEffect(loaded) { if (loaded && planIds == null) planIds = alarm?.challengePlan().orEmpty().toCsv() }
                val plan = remember(planIds) { planIds?.let(ChallengeType::parse) }
                val canSnooze by RingingService.canSnooze.collectAsState()
                if (dismissed) {
                    AlarmDismissed {
                        startActivity(Intent(this@RingingActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    }
                } else {
                    RingingScreen(
                        alarm = alarm,
                        plan = plan,
                        canSnooze = canSnooze,
                        muted = muted,
                        onSnooze = { send(id, RingingService.ACTION_SNOOZE) },
                        onDismiss = { dismissed = true; send(id, RingingService.ACTION_STOP) },
                        onChallengesStarted = { mute(id, alarm, force = true) },
                        onInteract = { if (alarm?.restartMuteOnTouch == true) mute(id, alarm, force = false) },
                    )
                }
            } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // so a rotation afterwards re-reads this alarm's id, not the first one's
        alarmId.longValue = intent.getLongExtra(AlarmReceiver.EXTRA_ID, alarmId.longValue)
    }

    private fun send(id: Long, action: String) = startService(RingingService.intent(this, id, action))

    // Touch-driven restarts are throttled to once a second so typing doesn't spam the service.
    private fun mute(id: Long, alarm: Alarm?, force: Boolean) {
        val seconds = alarm?.muteSeconds ?: 0
        val now = SystemClock.elapsedRealtime()
        if (seconds <= 0 || (!force && now - lastMute < 1000)) return
        lastMute = now
        startService(RingingService.intent(this, id, RingingService.ACTION_MUTE).putExtra(RingingService.EXTRA_SECONDS, seconds))
    }

    companion object {
        fun intent(context: Context, id: Long): Intent =
            Intent(context, RingingActivity::class.java).putExtra(AlarmReceiver.EXTRA_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
