package com.nautesh.challengeclock.ui.alarms

import android.text.format.DateFormat
import androidx.compose.animation.animateContentSize
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nautesh.challengeclock.ui.LocalSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.produceState
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.ui.AppText
import com.nautesh.challengeclock.ui.LABEL_MAX_LENGTH
import com.nautesh.challengeclock.ui.MenuPill
import com.nautesh.challengeclock.ui.PillTextField
import com.nautesh.challengeclock.ui.SheetTitle
import com.nautesh.challengeclock.app
import com.nautesh.challengeclock.data.Alarm
import com.nautesh.challengeclock.data.ChallengeType
import com.nautesh.challengeclock.data.challengeTypes
import com.nautesh.challengeclock.data.toCsv
import com.nautesh.challengeclock.ui.clockDigits
import com.nautesh.challengeclock.ui.NewAlarmMorphKey
import com.nautesh.challengeclock.ui.TimeDraft
import com.nautesh.challengeclock.ui.sharedMorph
import com.nautesh.challengeclock.ui.rememberSoundPicker
import com.nautesh.challengeclock.ui.soundTitle
import com.nautesh.challengeclock.ui.TimeField
import com.nautesh.challengeclock.ui.isLandscape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.displayCutoutPadding
import java.time.format.TextStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(id: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val is24 = DateFormat.is24HourFormat(context)
    val vm = viewModel(key = "alarm-$id") { EditAlarmViewModel(context.app.alarms, id, is24, createSavedStateHandle()) }
    val draft = vm.draft
    val snackbar = LocalSnackbar.current
    // Close and Back ask first when they would throw edits away.
    var confirmDiscard by remember { mutableStateOf(false) }
    val close = { if (vm.hasChanges && !vm.busy) confirmDiscard = true else onClose() }
    BackHandler(onBack = close)
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            confirmButton = { TextButton({ confirmDiscard = false; onClose() }) { Text(stringResource(R.string.discard)) } },
            dismissButton = { TextButton({ confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) } },
        )
    }
    val save = {
        vm.save { saved ->
            onClose()
            alarmSetText(context, saved)?.let { snackbar?.show(it) }
        }
    }
    val delete = {
        vm.delete { gone ->
            onClose()
            snackbar?.show(resources.getString(R.string.alarm_deleted), resources.getString(R.string.undo)) {
                context.app.scope.launch { context.app.alarms.save(gone) }
            }
        }
    }

    Scaffold(
        // No bottom inset here: the form scrolls behind the gesture bar (its own scroll padding clears it).
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (id == 0L) R.string.new_alarm else R.string.edit_alarm), style = AppText.sectionTitle) },
                navigationIcon = {
                    IconButton(close) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        // Save floats bottom-right, with Delete (existing alarms only) floating above it. Opening "New alarm" from the
        // + menu morphs that pill into Save.
        floatingActionButton = {
          Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (id != 0L) {
                // Same 56dp pill as the + menu's, in the destructive color.
                MenuPill(
                    painterResource(R.drawable.ic_delete),
                    stringResource(R.string.delete_alarm),
                    color = MaterialTheme.colorScheme.errorContainer,
                    enabled = draft != null && !vm.busy,
                    onClick = delete,
                )
            }
            Surface(
                onClick = save,
                enabled = draft != null && !vm.busy && vm.timeComplete,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer, // same as the nav bar and the + menu
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.sharedMorph(NewAlarmMorphKey, RoundedCornerShape(24.dp)).height(80.dp),
            ) {
                Row(Modifier.padding(start = 26.dp, end = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(28.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.save), style = AppText.prominent)
                }
            }
          }
        },
    ) { padding ->
        if (draft == null) return@Scaffold
        val landscape = isLandscape()
        // Time, label and repeat days; in landscape they hold still in the left column while the rest scrolls.
        val basics = @Composable {
            TimeInput(vm.time, is24)
            val labelName = stringResource(R.string.label)
            PillTextField(
                value = draft.label,
                onValueChange = { text -> vm.update { it.copy(label = text.take(LABEL_MAX_LENGTH)) } },
                modifier = Modifier.fillMaxWidth().padding(top = if (landscape) 12.dp else 40.dp).semantics { contentDescription = labelName },
                placeholder = stringResource(R.string.alarm),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            SectionTitle(stringResource(R.string.repeat), daysLabel(draft.days))
            RepeatDays(draft.days) { days -> vm.update { it.copy(days = days) } }
        }
        val scrolling = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
        val clearFabs = if (id != 0L) 180.dp else 112.dp // room to scroll past the floating Delete and Save
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(padding).imePadding().displayCutoutPadding()) {
                Column(Modifier.width(404.dp).then(scrolling).padding(bottom = 12.dp)) { basics() }
                Column(Modifier.weight(1f).then(scrolling).padding(bottom = clearFabs)) {
                    Challenges(draft, vm::update)
                    SectionTitle(stringResource(R.string.options))
                    Options(draft, vm::update)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).imePadding().then(scrolling).padding(bottom = clearFabs)) {
                basics()
                Challenges(draft, vm::update)
                SectionTitle(stringResource(R.string.options))
                Options(draft, vm::update)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, trailing: String? = null) {
    Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 28.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, Modifier.weight(1f), style = AppText.formSection)
        if (trailing != null) {
            Text(trailing, style = AppText.sectionCount, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeInput(time: TimeDraft, is24: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
    ) {
        TimeField(time.hourText, time::setHour, time::tidyHour, stringResource(R.string.hour), digitWidth = 70f, digitWeight = 780)
        Text(":", Modifier.padding(horizontal = 6.dp), style = clockDigits(60.sp, width = 70f).copy(lineHeight = 96.sp, fontWeight = FontWeight(780)))
        TimeField(time.minuteText, time::setMinute, time::tidyMinute, stringResource(R.string.minute), digitWidth = 70f, digitWeight = 780)
        if (!is24) {
            Column(Modifier.padding(start = 8.dp).size(width = 60.dp, height = 96.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(false to meridiem(0), true to meridiem(12)).forEachIndexed { i, (isPm, label) ->
                    ToggleButton(
                        checked = time.pm == isPm,
                        onCheckedChange = { time.pm = isPm },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shapes = (if (i == 0) RoundedCornerShape(16.dp, 16.dp, 6.dp, 6.dp) else RoundedCornerShape(6.dp, 6.dp, 16.dp, 16.dp))
                            .let { ToggleButtonShapes(shape = it, pressedShape = it, checkedShape = it) },
                        colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        contentPadding = PaddingValues(0.dp),
                    ) { Text(label, style = AppText.choiceLabel) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RepeatDays(days: Int, onChange: (Int) -> Unit) {
    val week = localeWeek()
    val locale = LocalConfiguration.current.locales[0]
    // Connected button group: one bar of seven, outer ends rounded; a selected day becomes a full pill.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        week.forEachIndexed { i, day ->
            val on = days and day.bit() != 0
            ToggleButton(
                checked = on,
                onCheckedChange = { onChange(days xor day.bit()) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    week.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(0.dp),
                colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) { Text(day.getDisplayName(TextStyle.NARROW, locale), style = AppText.choiceLabel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Challenges(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit) {
    val chosen = draft.challengeTypes
    var adding by rememberSaveable { mutableStateOf(false) }
    val available = ChallengeType.entries - chosen.toSet()

    SectionTitle(
        stringResource(R.string.challenges),
        if (chosen.isEmpty()) stringResource(R.string.challenges_none) else stringResource(R.string.challenges_added, chosen.size),
    )
    // Grows and shrinks smoothly as challenges are added or removed.
    Column(Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        val rows = chosen.size + 1
        chosen.forEachIndexed { i, type ->
            Surface(shape = segmentShape(i, rows), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                ChallengeRow(type) {
                    val name = stringResource(type.title)
                    IconButton({
                        update { a ->
                            val next = a.challengeTypes - type
                            a.copy(challenges = next.toCsv(), challengeCount = a.challengeCount.coerceAtMost(next.size))
                        }
                    }) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.remove_challenge, name)) }
                }
            }
        }
        Surface(
            onClick = { adding = true },
            enabled = available.isNotEmpty(),
            shape = segmentShape(chosen.size, rows),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_add), null, Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.add_challenge), style = AppText.choiceLabel)
            }
        }
    }

    if (chosen.isNotEmpty()) ChallengeSettings(draft, chosen.size, update)

    if (adding) {
        ModalBottomSheet(onDismissRequest = { adding = false }, containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SheetTitle(stringResource(R.string.add_challenge))
                available.forEachIndexed { i, type ->
                    Surface(
                        onClick = {
                            adding = false
                            update { a ->
                                val next = a.challengeTypes + type
                                // Keep "solve all" if it was solve-all before adding.
                                val count = if (a.challengeCount == 0 || a.challengeCount == a.challengeTypes.size) next.size else a.challengeCount
                                a.copy(challenges = next.toCsv(), challengeCount = count)
                            }
                        },
                        shape = segmentShape(i, available.size),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) { ChallengeRow(type) {} }
                }
            }
        }
    }
}

@Composable
private fun ChallengeRow(type: ChallengeType, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        ChallengeAvatar(type)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(stringResource(type.title), style = AppText.rowTitle)
            Text(stringResource(type.description), style = AppText.rowDetail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun ChallengeSettings(draft: Alarm, chosen: Int, update: ((Alarm) -> Alarm) -> Unit) {
    SectionTitle(stringResource(R.string.challenge_settings))
    val muteOn = draft.muteSeconds > 0
    val pickCount = chosen >= 2
    val rows = 1 + (if (pickCount) 2 else 0) + (if (muteOn) 2 else 0)
    var row = 0
    // Mute duration / restart rows slide in and out with the Mute switch.
    Column(Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        // Shuffle and count only mean something with two or more challenges.
        if (pickCount) {
            SwitchRow(stringResource(R.string.shuffle_challenges), null, draft.shuffleChallenges, segmentShape(row++, rows)) { on ->
                update { it.copy(shuffleChallenges = on) }
            }
            val count = draft.challengeCount.takeIf { it in 1..chosen } ?: chosen
            SliderRow(stringResource(R.string.number_of_challenges), count.toString(), count.toFloat(), 1f..chosen.toFloat(), chosen - 2, segmentShape(row++, rows)) { v ->
                update { it.copy(challengeCount = v.roundToInt()) }
            }
        }
        SwitchRow(stringResource(R.string.mute_alarm), stringResource(R.string.mute_alarm_desc), muteOn, segmentShape(row++, rows)) { on ->
            update { it.copy(muteSeconds = if (on) 30 else 0) }
        }
        if (muteOn) {
            SliderRow(stringResource(R.string.mute_duration), stringResource(R.string.seconds, draft.muteSeconds), draft.muteSeconds.toFloat(), 10f..120f, 10, segmentShape(row++, rows)) { v ->
                update { it.copy(muteSeconds = (v / 10).roundToInt() * 10) }
            }
            SwitchRow(stringResource(R.string.restart_mute), stringResource(R.string.restart_mute_desc), draft.restartMuteOnTouch, segmentShape(row++, rows)) { on ->
                update { it.copy(restartMuteOnTouch = on) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Options(draft: Alarm, update: ((Alarm) -> Alarm) -> Unit) {
    val context = LocalContext.current
    val pickSound = rememberSoundPicker(draft.sound) { sound -> update { it.copy(sound = sound) } }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Surface(onClick = pickSound, shape = segmentShape(0, 3), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.sound), style = AppText.rowTitle)
                // The title is a ContentResolver query: off the main thread.
                val title by produceState(stringResource(R.string.sound_default), draft.sound) {
                    value = withContext(Dispatchers.IO) { soundTitle(context, draft.sound) }
                }
                Text(
                    title,
                    style = AppText.rowDetail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SwitchRow(stringResource(R.string.vibrate), null, draft.vibrate, segmentShape(1, 3), MaterialTheme.colorScheme.surfaceContainerLow) { on -> update { it.copy(vibrate = on) } }
        Surface(shape = segmentShape(2, 3), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.snooze_length), style = AppText.rowTitle)
                ConnectedChoices(listOf(5, 10, 15, 20), draft.snoozeMinutes, { stringResource(R.string.duration_min, it) }) { minutes ->
                    update { it.copy(snoozeMinutes = minutes) }
                }
                Text(stringResource(R.string.snooze_limit), Modifier.padding(top = 20.dp), style = AppText.rowTitle)
                // 0 = no limit
                ConnectedChoices(listOf(1, 3, 5, 0), draft.snoozeLimit, {
                    if (it == 0) stringResource(R.string.snooze_unlimited) else stringResource(R.string.snooze_times, it)
                }) { limit -> update { it.copy(snoozeLimit = limit) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectedChoices(choices: List<Int>, selected: Int, label: @Composable (Int) -> String, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        choices.forEachIndexed { i, value ->
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
                modifier = Modifier.weight(1f).height(44.dp),
                // These sit on a card, so unselected ones take onPrimary (near white) to stand out from it.
                colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.onPrimary),
                contentPadding = PaddingValues(horizontal = 4.dp), // room for "No limit" in a quarter of a phone's width
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    choices.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(
                    label(value),
                    maxLines = 1,
                    style = AppText.choiceLabel,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, shape: androidx.compose.ui.graphics.Shape, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow, onChange: (Boolean) -> Unit) {
    Surface(
        checked = checked,
        onCheckedChange = onChange,
        shape = shape,
        color = color,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = AppText.rowTitle)
                if (subtitle != null) Text(subtitle, style = AppText.rowDetail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange = null, colors = appSwitchColors())
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    shape: androidx.compose.ui.graphics.Shape,
    onChange: (Float) -> Unit,
) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row {
                Text(title, Modifier.weight(1f), style = AppText.rowTitle)
                Text(valueLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            val state = remember(range, steps) { SliderState(value, steps.coerceAtLeast(0), range) }
            LaunchedEffect(value) { state.value = value }
            Slider(
                state,
                onValueChange = { v -> state.value = v; onChange(v) },
                enabled = range.endInclusive > range.start,
                // outlineVariant keeps the inactive track visible against the row.
                colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}
