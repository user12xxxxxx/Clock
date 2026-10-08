package com.nautesh.challengeclock

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.room.Room
import com.nautesh.challengeclock.alarm.AlarmScheduler
import com.nautesh.challengeclock.alarm.RingingService
import com.nautesh.challengeclock.alarm.SnoozeNotifier
import com.nautesh.challengeclock.alarm.TimerNotifier
import com.nautesh.challengeclock.data.AlarmDatabase
import com.nautesh.challengeclock.data.AlarmRepository
import com.nautesh.challengeclock.data.CountdownRepository
import com.nautesh.challengeclock.data.StopwatchStore
import com.nautesh.challengeclock.data.WorldClocks
import com.nautesh.challengeclock.ui.fontAssets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    private val db by lazy {
        Room.databaseBuilder(this, AlarmDatabase::class.java, "alarms.db")
            .addMigrations(AlarmDatabase.MIGRATION_1_2, AlarmDatabase.MIGRATION_2_3, AlarmDatabase.MIGRATION_3_4, AlarmDatabase.MIGRATION_4_5)
            .build()
    }
    // Which boot this is (0 if the device doesn't say); elapsed-time stamps are only comparable within one boot.
    private val bootCount by lazy { Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0) }
    val scheduler by lazy { AlarmScheduler(this) }
    val alarms by lazy { AlarmRepository(db.alarms(), scheduler, SnoozeNotifier(this)) }
    val timers by lazy {
        CountdownRepository(db.timers(), scheduler, TimerNotifier(this), bootCount) { id ->
            if (RingingService.ringingTimerId.value == id) {
                startService(RingingService.intent(this, id, RingingService.ACTION_STOP_TIMER))
            }
        }
    }

    val worldClocks by lazy { WorldClocks(getSharedPreferences("world_clocks", MODE_PRIVATE)) }
    val stopwatch by lazy {
        StopwatchStore(getSharedPreferences("stopwatch", MODE_PRIVATE), bootCount)
    }
    private val settings by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    /** Ringtone URI for finished timers; empty means the system's default alarm sound. */
    var timerSound: String
        get() = settings.getString("timer_sound", "")!!
        set(value) = settings.edit().putString("timer_sound", value).apply()

    /** Full-screen clock and stopwatch on a true black page (for OLED screens). */
    var fullScreenBlack: Boolean
        get() = settings.getBoolean("full_black", false)
        set(value) = settings.edit().putBoolean("full_black", value).apply()

    /** Full-screen stopwatch without the hundredths of a second. */
    var fullScreenHideHundredths: Boolean
        get() = settings.getBoolean("full_hide_cs", false)
        set(value) = settings.edit().putBoolean("full_hide_cs", value).apply()

    /**
     * For writes started from the UI. A screen's own scope dies when you navigate away, which could cut a write
     * off between the row and its AlarmManager entry; this one lives as long as the process.
     */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        fontAssets = assets
        // A force-stop (by the user or an aggressive OEM) wipes our AlarmManager entries; re-arm on every start.
        scope.launch {
            alarms.rescheduleAll()
            timers.rescheduleAll()
        }
    }
}

val Context.app: App get() = applicationContext as App
