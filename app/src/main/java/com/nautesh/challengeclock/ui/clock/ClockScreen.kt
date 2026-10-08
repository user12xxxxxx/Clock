package com.nautesh.challengeclock.ui.clock

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.ui.AppText
import com.nautesh.challengeclock.ui.EmptyState
import com.nautesh.challengeclock.ui.PillTextField
import com.nautesh.challengeclock.ui.SectionHeader
import com.nautesh.challengeclock.ui.SheetTitle
import com.nautesh.challengeclock.app
import com.nautesh.challengeclock.data.WorldClocks
import com.nautesh.challengeclock.ui.clockDigits
import com.nautesh.challengeclock.ui.ScreenHeader
import com.nautesh.challengeclock.ui.TwoPane
import com.nautesh.challengeclock.ui.FullScreenTime
import com.nautesh.challengeclock.ui.isLandscape
import com.nautesh.challengeclock.ui.alarms.segmentShape
import com.nautesh.challengeclock.ui.alarms.longDate
import com.nautesh.challengeclock.ui.alarms.clockText
import com.nautesh.challengeclock.ui.LocalSnackbar
import androidx.compose.ui.res.pluralStringResource
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ClockScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val world = context.app.worldClocks
    val zones by world.zones.collectAsState()
    val snackbar = LocalSnackbar.current
    val is24 = DateFormat.is24HourFormat(context)
    // Tick on each second boundary so the seconds never skip or stutter.
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            value = ZonedDateTime.now()
        }
    }
    var adding by rememberSaveable { mutableStateOf(false) }
    var full by rememberSaveable { mutableStateOf(false) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Landscape: the local clock fills its own pane beside the World list.
    val landscape = isLandscape()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
      Box {
       val list = @Composable {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = top + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 112.dp, // room to scroll past the + button
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item { ScreenHeader(stringResource(R.string.tab_clock_title), onFullScreen = { full = true }) }
            if (!landscape) item { LocalClockFace(now, is24) }
            item { SectionHeader(stringResource(R.string.world), pluralStringResource(R.plurals.cities_count, zones.size, zones.size)) }
            if (zones.isEmpty()) item { EmptyState(stringResource(R.string.no_cities)) }
            itemsIndexed(zones, key = { _, z -> z }) { i, zone ->
                key(zone) {
                    val state = rememberSwipeToDismissBoxState()
                    LaunchedEffect(state.currentValue) {
                        if (state.currentValue != SwipeToDismissBoxValue.EndToStart) return@LaunchedEffect
                        world.remove(zone)
                        snackbar?.show(resources.getString(R.string.city_removed, WorldClocks.cityName(zone)), resources.getString(R.string.undo)) {
                            world.add(zone, at = i)
                        }
                    }
                    SwipeToDismissBox(
                        modifier = Modifier.animateItem(),
                        state = state,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().clip(segmentShape(i, zones.size)).background(MaterialTheme.colorScheme.errorContainer),
                            )
                        },
                    ) {
                        CityRow(zone, now, is24, segmentShape(i, zones.size))
                    }
                }
            }
        }
       }
        if (landscape) TwoPane(contentPadding, hero = { LocalClockFace(now, is24) }) { list() } else list()
        // Same + button as Alarms and Timer (bottom-right, above the nav bar); one action, so it opens Add city directly.
        Surface(
            onClick = { adding = true },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.align(Alignment.BottomEnd).padding(contentPadding).padding(end = 16.dp, bottom = 8.dp).size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.add_city), Modifier.size(32.dp))
            }
        }
      }
    }

    if (full) {
        // A clock always runs, so it takes the running (tertiaryContainer) page. Hours are padded here ("07"), as drawn.
        val time = clockText(now.hour, now.minute, is24)
        FullScreenTime(
            groups = listOf(time.hour.padStart(2, '0'), time.minute, "%02d".format(now.second)),
            accentLast = true,
            running = true,
            spoken = listOfNotNull("${time.hour}:${time.minute}", time.meridiem).joinToString(" "),
            title = stringResource(R.string.clock_full_screen),
            onExit = { full = false },
            meridiem = time.meridiem,
        )
    }

    if (adding) AddCitySheet(zones, now, is24, onDismiss = { adding = false }) { zone ->
        world.add(zone)
        adding = false
    }
}

@Composable
private fun LocalClockFace(now: ZonedDateTime, is24: Boolean) {
    val c = MaterialTheme.colorScheme
    val time = clockText(now.hour, now.minute, is24)
    val place = WorldClocks.cityName(ZoneId.systemDefault().id)
    val date = longDate(now.toLocalDate())
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // As tuned in the design: wide (width 55) digits, right-aligned with a 28dp inset; seconds and
        // AM/PM sit on the same baseline in the same face. One line that shrinks to fit: "12:07:00 PM"
        // at 116sp is wider than a phone, and as separate Texts the AM/PM wrapped letter by letter.
        // Seconds and AM/PM are sized in em (40/116, 26/116) so they scale with the shrink.
        val big = clockDigits(116.sp, width = 55f)
        val secs = clockDigits(40.sp, width = 55f)
        val small = clockDigits(26.sp, width = 55f)
        BasicText(
            buildAnnotatedString {
                append("${time.hour}:${time.minute}")
                withStyle(SpanStyle(fontFamily = secs.fontFamily, fontSize = 0.345.em, letterSpacing = (-0.5).sp, fontWeight = FontWeight(750), color = c.primary)) {
                    append(":%02d".format(now.second))
                }
                if (time.meridiem != null) {
                    withStyle(SpanStyle(fontFamily = small.fontFamily, fontSize = 0.224.em, letterSpacing = 0.sp, fontWeight = FontWeight(750), color = c.primary)) {
                        append(" ${time.meridiem}")
                    }
                }
            },
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 28.dp),
            style = big.copy(lineHeight = 116.sp, letterSpacing = (-1.5).sp, fontWeight = FontWeight(820), color = c.onPrimaryContainer, textAlign = TextAlign.End),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 48.sp, maxFontSize = 116.sp),
        )
        Text(
            stringResource(R.string.dot_join, date, place),
            Modifier.padding(top = 8.dp),
            style = AppText.subtitle,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun relativeText(zoneNow: ZonedDateTime, localNow: ZonedDateTime): String {
    val days = ChronoUnit.DAYS.between(localNow.toLocalDate(), zoneNow.toLocalDate())
    val day = stringResource(
        when { days > 0 -> R.string.tomorrow; days < 0 -> R.string.yesterday; else -> R.string.today },
    )
    val diffMin = (zoneNow.offset.totalSeconds - localNow.offset.totalSeconds) / 60
    if (diffMin == 0) return stringResource(R.string.dot_join, day, stringResource(R.string.same_time))
    val span = listOfNotNull(
        (abs(diffMin) / 60).takeIf { it > 0 }?.let { stringResource(R.string.duration_h, it) },
        (abs(diffMin) % 60).takeIf { it > 0 }?.let { stringResource(R.string.duration_min, it) },
    ).joinToString(" ")
    return stringResource(R.string.dot_join, day, stringResource(if (diffMin > 0) R.string.ahead else R.string.behind, span))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CityRow(
    zone: String,
    now: ZonedDateTime,
    is24: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    val there = now.withZoneSameInstant(ZoneId.of(zone))
    val day = there.hour in 6..17
    val time = clockText(there.hour, there.minute, is24)
    val content = @Composable {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(MaterialShapes.Cookie4Sided.toShape())
                    .background(if (day) c.tertiaryContainer else c.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (day) R.drawable.ic_sun else R.drawable.ic_moon),
                    stringResource(if (day) R.string.daytime else R.string.nighttime),
                    tint = if (day) c.onTertiaryContainer else c.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(WorldClocks.cityName(zone), style = AppText.rowTitle)
                Text(
                    relativeText(there, now),
                    Modifier.padding(top = 2.dp),
                    style = AppText.rowDetail,
                    color = c.onSurfaceVariant,
                )
            }
            Row {
                Text("${time.hour}:${time.minute}", Modifier.alignByBaseline(), style = clockDigits(32.sp, width = 60f).copy(lineHeight = 32.sp, fontWeight = FontWeight(780)))
                if (time.meridiem != null) {
                    Text(
                        time.meridiem,
                        Modifier.alignByBaseline().padding(start = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight(700)),
                        color = c.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (onClick == null) Surface(shape = shape, color = color, content = content)
    else Surface(onClick = onClick, shape = shape, color = color, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCitySheet(current: List<String>, now: ZonedDateTime, is24: Boolean, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = WorldClocks.pickable
        .filter { it !in current && (query.isBlank() || WorldClocks.cityName(it).contains(query.trim(), ignoreCase = true) || it.contains(query.trim(), ignoreCase = true)) }
        .take(60)
    // The list runs edge to edge under the system navigation bar; only its bottom padding clears the bar.
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded)),
        contentWindowInsets = { WindowInsets.statusBars },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            SheetTitle(stringResource(R.string.add_city))
            PillTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.search_cities),
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
            )
            // Same cards as the World list, so a city looks the same before and after adding it.
            LazyColumn(
                Modifier.padding(top = 12.dp),
                contentPadding = PaddingValues(bottom = navBar + 16.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (matches.isEmpty()) {
                    item { EmptyState(stringResource(R.string.no_cities_match)) }
                }
                itemsIndexed(matches, key = { _, zone -> zone }) { i, zone ->
                    CityRow(zone, now, is24, segmentShape(i, matches.size)) { onPick(zone) }
                }
            }
        }
    }
}
