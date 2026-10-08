package com.nautesh.challengeclock.ui.alarms

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.ui.AppText
import com.nautesh.challengeclock.ui.EmptyState
import com.nautesh.challengeclock.ui.SectionHeader
import com.nautesh.challengeclock.app
import com.nautesh.challengeclock.data.Alarm
import com.nautesh.challengeclock.data.nextRing
import com.nautesh.challengeclock.data.snoozedAt
import com.nautesh.challengeclock.ui.FabAction
import com.nautesh.challengeclock.ui.FabMenu
import com.nautesh.challengeclock.ui.NewAlarmMorphKey
import com.nautesh.challengeclock.ui.HeroCardShape
import com.nautesh.challengeclock.ui.LocalSnackbar
import com.nautesh.challengeclock.ui.ScreenHeader
import com.nautesh.challengeclock.ui.TwoPane
import com.nautesh.challengeclock.ui.isLandscape
import com.nautesh.challengeclock.ui.clockDigits
import com.nautesh.challengeclock.ui.sharedMorph
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmsScreen(contentPadding: PaddingValues, onOpenAlarm: (Long) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val repo = context.app.alarms
    val scope = context.app.scope
    val snackbar = LocalSnackbar.current
    val loaded by repo.all.collectAsState(null)
    val alarms = loaded.orEmpty()
    val is24 = DateFormat.is24HourFormat(context)
    // Alarms ring on whole minutes, so "Next alarm in" only changes on a minute boundary: tick exactly then.
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            value = ZonedDateTime.now()
        }
    }

    RequestNotificationPermission()

    // The hero is whichever alarm rings next (a pending snooze counts); with everything off, the first alarm stands in.
    val next = alarms.mapNotNull { a -> a.nextRing(now)?.let { a to it } }.minByOrNull { it.second }
    val hero = next?.first ?: alarms.firstOrNull()
    val rest = alarms.filter { it.id != hero?.id }
    val subtitle = when {
        next != null -> stringResource(R.string.next_alarm_in, durationText(context, Duration.between(now, next.second)))
        alarms.isEmpty() -> null
        else -> stringResource(R.string.all_alarms_off)
    }
    // A snoozed alarm reads as on (it will ring); switching it off also drops the snooze.
    val toggle: (Alarm) -> Unit = { a -> scope.launch { repo.save(a.copy(enabled = !a.isOn(now))) } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Landscape: the hero sits in its own pane beside the list rather than at the top of it.
    val landscape = isLandscape()
    // Morph: when the next alarm changes, its tile grows into this card and the old hero shrinks back into its
    // tile. Both sides carry the same shared key, "alarm-<id>".
    val heroCard = @Composable { modifier: Modifier ->
        if (hero != null) AnimatedContent(hero, modifier, transitionSpec = { fadeIn() togetherWith fadeOut() }, contentKey = { it.id }) { a ->
            HeroAlarmCard(
                a, now, is24, onToggle = { toggle(a) }, onClick = { onOpenAlarm(a.id) },
                Modifier.sharedMorph("alarm-${a.id}", HeroCardShape, this),
            )
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Box {
          val grid = @Composable {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = top + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 112.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                // No item spacing: the hero's own tile stays in the grid at zero height (see below), and spacing
                // would leave a gap for it. Items pad their own bottom instead.
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ScreenHeader(stringResource(R.string.tab_alarm_title), subtitle) {
                        IconButton({ openBedtimeSettings(context) }) { Icon(painterResource(R.drawable.ic_moon), stringResource(R.string.bedtime_mode)) }
                    }
                }
                item(span = StaggeredGridItemSpan.FullLine) { Box(Modifier.padding(bottom = 12.dp)) { PermissionBanners() } }
                if (hero == null) {
                    // Only once the list has loaded: an empty first frame shouldn't flash "No alarms yet".
                    if (loaded != null) item(span = StaggeredGridItemSpan.FullLine) { EmptyState(stringResource(R.string.no_alarms)) }
                } else if (!landscape) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "hero") { heroCard(Modifier.animateItem().padding(bottom = 12.dp)) }
                }
                if (rest.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        SectionHeader(stringResource(R.string.all_alarms), stringResource(R.string.more_count, rest.size), Modifier.animateItem())
                    }
                    // Every alarm keeps a grid item; the hero's is hidden. Hiding (not removing) it lets the tile
                    // play its half of the morph as it leaves, and lets the old hero's tile morph back in.
                    items(alarms, key = { it.id }) { alarm ->
                        // animateItem: neighbours slide (not jump) as tiles hide and reappear.
                        AnimatedVisibility(alarm.id != hero?.id, Modifier.animateItem(), enter = fadeIn(), exit = fadeOut()) {
                            AlarmTile(
                                alarm, now, is24, onToggle = { toggle(alarm) }, onClick = { onOpenAlarm(alarm.id) },
                                Modifier.padding(bottom = 12.dp).sharedMorph("alarm-${alarm.id}", RoundedCornerShape(28.dp), this),
                            )
                        }
                    }
                }
            }
          }
            if (landscape) TwoPane(contentPadding, hero = { heroCard(Modifier) }) { grid() } else grid()
            CreateMenu(
                onNewAlarm = { onOpenAlarm(0) },
                onQuickNap = {
                    // Rounded up to the next whole minute, so the nap is never shorter than 20 minutes.
                    val at = LocalTime.now().plusMinutes(20).plusSeconds(59).truncatedTo(ChronoUnit.MINUTES)
                    val label = resources.getString(R.string.nap)
                    // Reuses the last nap alarm rather than leaving a new disabled "Nap" behind each time.
                    val previous = alarms.firstOrNull { it.label == label && it.days == 0 }
                    scope.launch {
                        val nap = repo.save(previous?.copy(hour = at.hour, minute = at.minute, enabled = true) ?: Alarm(hour = at.hour, minute = at.minute, label = label))
                        alarmSetText(context, nap)?.let { snackbar?.show(it) }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(contentPadding),
            )
        }
    }
}


// TalkBack label for an alarm's switch: "Gym alarm at 7:30 AM", or "Alarm at 7:30" with no label (not "Alarm alarm").
@Composable
private fun switchLabel(alarm: Alarm, time: ClockText): String {
    val at = listOfNotNull("${time.hour}:${time.minute}", time.meridiem).joinToString(" ")
    return if (alarm.label.isBlank()) stringResource(R.string.alarm_at, at) else stringResource(R.string.alarm_toggle, alarm.label, at)
}

/** On = will ring: enabled, or snoozed (a one-time alarm is already off while its snooze is pending). */
private fun Alarm.isOn(now: ZonedDateTime) = enabled || snoozedAt(now)

// "Snoozed until 7:10" while a snooze is pending, otherwise the repeat days.
@Composable
private fun detailLabel(alarm: Alarm, now: ZonedDateTime): String =
    if (alarm.snoozedAt(now)) {
        stringResource(R.string.snoozed_until, DateFormat.getTimeFormat(LocalContext.current).format(Date(alarm.snoozedUntil)))
    } else {
        daysLabel(alarm.days)
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroAlarmCard(alarm: Alarm, now: ZonedDateTime, is24: Boolean, onToggle: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val on = alarm.isOn(now)
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<androidx.compose.ui.graphics.Color>()
    val container by animateColorAsState(if (on) c.primary else c.surfaceContainerLow, effects)
    val content by animateColorAsState(if (on) c.onPrimary else c.onSurfaceVariant, effects)
    // Decoration and label chip are small marks: errorContainer in both states.
    val deco = c.errorContainer
    val chip = c.errorContainer
    val decoTurn by animateFloatAsState(if (on) 40f else 0f, MaterialTheme.motionScheme.slowSpatialSpec())
    val weight by animateIntAsState(if (on) 800 else 400, MaterialTheme.motionScheme.defaultEffectsSpec())
    val time = clockText(alarm.hour, alarm.minute, is24)
    val switchLabel = switchLabel(alarm, time)

    Surface(onClick = onClick, shape = HeroCardShape, color = container, contentColor = content, modifier = modifier) {
        Box {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 72.dp, y = (-86).dp)
                    .size(230.dp)
                    .rotate(decoTurn)
                    .clip(MaterialShapes.Sunny.toShape())
                    .background(deco),
            )
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = chip, contentColor = c.onSurface) {
                        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_tab_alarm), null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(alarm.label.ifBlank { stringResource(R.string.alarm) }, style = AppText.choiceLabel)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    LabeledSwitch(
                        checked = on,
                        label = switchLabel,
                        onToggle = onToggle,
                    )
                }
                Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${time.hour}:${time.minute}",
                        style = clockDigits(116.sp).copy(lineHeight = 100.sp, letterSpacing = (-2).sp, fontWeight = FontWeight(weight)),
                    )
                    if (time.meridiem != null) {
                        Text(
                            time.meridiem,
                            Modifier.padding(start = 10.dp, bottom = 12.dp),
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight(weight)),
                        )
                    }
                }
                Text(
                    detailLabel(alarm, now),
                    Modifier.padding(top = 12.dp),
                    style = AppText.cardLabel,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlarmTile(alarm: Alarm, now: ZonedDateTime, is24: Boolean, onToggle: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val on = alarm.isOn(now)
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<androidx.compose.ui.graphics.Color>()
    val container by animateColorAsState(if (on) c.primary else c.surfaceContainerLow, effects)
    val content by animateColorAsState(if (on) c.onPrimary else c.onSurfaceVariant, effects)
    val weight by animateIntAsState(if (on) 800 else 400, MaterialTheme.motionScheme.defaultEffectsSpec())
    val time = clockText(alarm.hour, alarm.minute, is24)
    val switchLabel = switchLabel(alarm, time)

    // Pressed: the tile tightens its corners and shrinks a little, as in the design.
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 20.dp else 28.dp, MaterialTheme.motionScheme.fastSpatialSpec())
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, MaterialTheme.motionScheme.fastSpatialSpec())
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        interactionSource = press,
        modifier = modifier.heightIn(min = 188.dp).graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        // 20dp start (design: 16dp padding + 4dp inset) so digits, label and days share one edge.
        Column(Modifier.padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
            // Digits share the top row with the switch; AM/PM rides small after the minutes.
            Row {
                Text(
                    buildAnnotatedString {
                        append("${time.hour}\n${time.minute}")
                        if (time.meridiem != null) withStyle(SpanStyle(fontSize = 16.sp, letterSpacing = 0.sp)) { append(" ${time.meridiem}") }
                    },
                    Modifier.weight(1f),
                    style = clockDigits(58.sp, width = 38f).copy(lineHeight = 53.sp, letterSpacing = (-1).sp, fontWeight = FontWeight(weight)),
                )
                LabeledSwitch(on, switchLabel, onToggle)
            }
            Spacer(Modifier.weight(1f))
            Text(
                alarm.label.ifBlank { stringResource(R.string.alarm) },
                style = AppText.cardLabel.copy(fontWeight = FontWeight(weight)),
            )
            Text(
                detailLabel(alarm, now),
                Modifier.padding(top = 2.dp),
                style = AppText.cardDetail,
                color = content.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun CreateMenu(onNewAlarm: () -> Unit, onQuickNap: () -> Unit, modifier: Modifier = Modifier) {
    FabMenu(
        listOf(
            FabAction(R.drawable.ic_nap, stringResource(R.string.quick_nap), onClick = onQuickNap),
            FabAction(R.drawable.ic_tab_alarm, stringResource(R.string.new_alarm), NewAlarmMorphKey, onNewAlarm),
        ),
        modifier,
    )
}

/**
 * The system's Bedtime page: Digital Wellbeing's on phones that have it; otherwise the Modes page
 * (where Bedtime lives on Android 15+; Do Not Disturb on older versions).
 */
private fun openBedtimeSettings(context: Context) {
    listOf("android.settings.BEDTIME_SETTINGS", "android.settings.ZEN_MODE_SETTINGS").firstOrNull { action ->
        runCatching { context.startActivity(Intent(action)) }.isSuccess
    }
}

// Once per app launch, not on every visit to the tab; after a refusal the banner in PermissionBanners takes over.
private var askedForNotifications = false

@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (askedForNotifications) return@LaunchedEffect
        askedForNotifications = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun PermissionBanners() {
    val context = LocalContext.current
    // Reading the lifecycle state re-runs these checks when the user comes back from Settings.
    LocalLifecycleOwner.current.lifecycle.currentStateAsState().value
    val packageUri = remember { Uri.parse("package:${context.packageName}") }
    val notifications = context.getSystemService(NotificationManager::class.java)
    val canFullScreen = notifications.canUseFullScreenIntent()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Blocked notifications hide the ringing alert and its Stop/Snooze actions.
        if (!notifications.areNotificationsEnabled()) {
            Banner(stringResource(R.string.allow_notifications)) {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
        }
        if (!context.app.scheduler.canScheduleExact()) {
            Banner(stringResource(R.string.allow_exact_alarms)) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
            }
        }
        if (!canFullScreen) {
            Banner(stringResource(R.string.allow_full_screen)) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))
            }
        }
        if (Build.MANUFACTURER.lowercase() in aggressiveOems &&
            !context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        ) {
            // The settings list rather than the one-tap dialog: the dialog needs a permission Play restricts.
            Banner(stringResource(R.string.allow_background)) {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}

// Makers whose battery savers are known to kill alarm apps (see dontkillmyapp.com).
private val aggressiveOems = setOf("xiaomi", "samsung", "huawei", "honor", "oppo", "vivo", "realme", "oneplus", "meizu", "asus")

@Composable
private fun Banner(text: String, onAllow: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
        FilledTonalButton(onAllow) { Text(stringResource(R.string.allow)) }
    }
}
