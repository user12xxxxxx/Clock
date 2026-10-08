package com.nautesh.challengeclock.ui.alarms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nautesh.challengeclock.data.Alarm
import com.nautesh.challengeclock.data.AlarmRepository
import com.nautesh.challengeclock.ui.TimeDraft
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditAlarmViewModel(private val repo: AlarmRepository, val id: Long, is24: Boolean, private val state: SavedStateHandle) : ViewModel() {
    var draft by mutableStateOf<Alarm?>(null)
        private set
    val time = TimeDraft(is24)
    // The alarm as opened, to tell whether leaving would throw edits away.
    private var original: Alarm? = null

    /** True once Save or Delete is under way; the screen disables both so a double tap can't run them twice. */
    var busy by mutableStateOf(false)
        private set

    /** Both time fields hold a number; an emptied one would otherwise save silently as 00 (or 12). */
    val timeComplete: Boolean get() = time.hourText.isNotEmpty() && time.minuteText.isNotEmpty()

    val hasChanges: Boolean
        get() {
            val before = original ?: return false
            val now = draft ?: return false
            return now != before || !timeComplete || time.hour != before.hour || time.minute != before.minute
        }

    init {
        val restored = state.get<Alarm>(KEY_DRAFT)
        if (restored != null) {
            // Back after process death: carry on with the half-made edit.
            original = state[KEY_ORIGINAL]
            draft = restored
            time.restore(state[KEY_HOUR] ?: "", state[KEY_MINUTE] ?: "", state[KEY_PM] ?: false)
        } else {
            viewModelScope.launch {
                val alarm = repo.get(id) ?: Alarm(hour = 7, minute = 0)
                original = alarm
                state[KEY_ORIGINAL] = alarm
                draft = alarm
                time.set(alarm.hour, alarm.minute)
            }
        }
        viewModelScope.launch {
            snapshotFlow { listOf(draft, time.hourText, time.minuteText, time.pm) }.collect {
                state[KEY_DRAFT] = draft
                state[KEY_HOUR] = time.hourText
                state[KEY_MINUTE] = time.minuteText
                state[KEY_PM] = time.pm
            }
        }
    }

    fun update(change: (Alarm) -> Alarm) {
        draft = draft?.let(change)
    }

    /** [onDone] gets the alarm as saved, for the "Alarm set for…" confirmation. */
    fun save(onDone: (Alarm) -> Unit) {
        if (!timeComplete) return
        val alarm = draft?.copy(hour = time.hour, minute = time.minute, enabled = true) ?: return
        finishWith(alarm, onDone) { repo.save(it) }
    }

    /** [onDone] gets the alarm as it was before this edit, for Undo. */
    fun delete(onDone: (Alarm) -> Unit) {
        val alarm = draft ?: return
        finishWith(original ?: alarm, onDone) { repo.delete(alarm) }
    }

    private fun finishWith(alarm: Alarm, onDone: (Alarm) -> Unit, write: suspend (Alarm) -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            // Leaving the screen mid-write clears this ViewModel; the write must still finish, or the row
            // could be saved without its AlarmManager entry.
            withContext(NonCancellable) { write(alarm) }
            onDone(alarm)
        }
    }

    private companion object {
        const val KEY_DRAFT = "draft"
        const val KEY_ORIGINAL = "original"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_PM = "pm"
    }
}
