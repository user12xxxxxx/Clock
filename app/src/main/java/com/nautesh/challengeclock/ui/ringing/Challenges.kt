package com.nautesh.challengeclock.ui.ringing

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nautesh.challengeclock.R
import com.nautesh.challengeclock.ui.AppText
import com.nautesh.challengeclock.ui.PillTextField
import com.nautesh.challengeclock.ui.PrimaryButton
import com.nautesh.challengeclock.data.ChallengeType
import com.nautesh.challengeclock.data.sameWords
import com.nautesh.challengeclock.ui.LocalReduceMotion
import com.nautesh.challengeclock.ui.clockDigits
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Challenge(type: ChallengeType, onSolved: () -> Unit) {
    when (type) {
        ChallengeType.MATH -> MathChallenge(onSolved)
        ChallengeType.MEMORY -> MemoryChallenge(onSolved)
        ChallengeType.SHAPES -> ShapesChallenge(onSolved)
        ChallengeType.RETYPE -> RetypeChallenge(onSolved)
        ChallengeType.SHAKE -> ShakeChallenge(onSolved)
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier, error: Boolean = false) = Text(
    text,
    modifier,
    style = AppText.hint,
    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
)

@Composable
private fun MathChallenge(onSolved: () -> Unit) {
    val a = rememberSaveable { Random.nextInt(12, 60) }
    val b = rememberSaveable { Random.nextInt(12, 60) }
    var answer by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    val check = {
        if (answer.toIntOrNull() == a + b) onSolved() else { wrong = true; answer = "" }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("$a + $b", style = clockDigits(88.sp))
        // Caption under the field, as under the time fields elsewhere.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val caption = stringResource(R.string.answer)
            PillTextField(
                value = answer,
                onValueChange = { answer = it.filter(Char::isDigit).take(3); wrong = false },
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).semantics { contentDescription = caption },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight(700), textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { check() }),
            )
            Text(caption.uppercase(), style = AppText.fieldCaption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (wrong) Hint(stringResource(R.string.wrong_try_again), error = true)
        PrimaryButton(stringResource(R.string.check), check, Modifier.fillMaxWidth().padding(top = 4.dp), enabled = answer.isNotEmpty())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val Faces = listOf(MaterialShapes.Heart, MaterialShapes.Triangle, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny, MaterialShapes.Pentagon)

// Spoken names, so TalkBack users can match cards and follow the sequence.
private val FaceNames = listOf(R.string.shape_heart, R.string.shape_triangle, R.string.shape_clover, R.string.shape_sun, R.string.shape_pentagon)
private val PadNames = listOf(R.string.shape_square, R.string.shape_triangle, R.string.shape_sun, R.string.shape_heart)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun faceColor(face: Int): Color = with(MaterialTheme.colorScheme) {
    listOf(error, primary, tertiary, secondary, onSurface)[face]
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MemoryChallenge(onSolved: () -> Unit) {
    val cards = rememberSaveable { (0 until Faces.size).flatMap { listOf(it, it) }.shuffled() }
    // Matched pairs survive rotation; a half-open pair just closes again.
    var matched by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    val open = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()

    fun flip(i: Int) {
        if (i in matched || i in open || open.size == 2) return
        open += i
        if (open.size < 2) return
        val (x, y) = open.toList()
        if (cards[x] == cards[y]) {
            matched = matched + listOf(x, y)
            open.clear()
            if (matched.size == cards.size) onSolved()
        } else {
            scope.launch { delay(700); open.clear() }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint(stringResource(R.string.memory_hint))
        cards.indices.chunked(5).forEach { row ->
            // Cards shrink to fit: five 60dp cards and their gaps need 332dp, more than a 360dp phone has inside the padding.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                row.forEach { i ->
                    val shown = i in open || i in matched
                    val label = if (shown) {
                        stringResource(R.string.memory_card_face, i + 1, stringResource(FaceNames[cards[i]]))
                    } else {
                        stringResource(R.string.memory_card, i + 1)
                    }
                    val bg by animateColorAsState(
                        if (shown) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                        MaterialTheme.motionScheme.defaultEffectsSpec(),
                    )
                    Surface(
                        onClick = { flip(i) },
                        shape = RoundedCornerShape(16.dp),
                        color = bg,
                        modifier = Modifier.weight(1f, fill = false).widthIn(max = 60.dp).aspectRatio(60f / 84f).semantics {
                            contentDescription = label
                        },
                    ) {
                        if (shown) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(Modifier.fillMaxWidth(2 / 3f).aspectRatio(1f).clip(Faces[cards[i]].toShape()).background(faceColor(cards[i])))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShapesChallenge(onSolved: () -> Unit) {
    val length = 5
    var round by rememberSaveable { mutableIntStateOf(0) }
    val sequence = remember(round) { List(length) { Random.nextInt(4) } }
    var lit by remember { mutableIntStateOf(-1) }
    var showing by remember(round) { mutableStateOf(true) }
    var position by remember(round) { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    // Step of the sequence last shown, for TalkBack (it stays put between flashes so each step is read once).
    var step by remember(round) { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(round) {
        delay(700)
        for ((n, pad) in sequence.withIndex()) {
            lit = pad
            step = n
            delay(550)
            lit = -1
            delay(200)
        }
        showing = false
    }

    fun tap(pad: Int) {
        if (showing || position >= length) return
        scope.launch { lit = pad; delay(180); if (lit == pad) lit = -1 }
        if (sequence[position] == pad) {
            failed = false
            position++
            if (position == length) onSolved()
        } else {
            failed = true
            round++
        }
    }

    val pads = listOf(MaterialShapes.Cookie4Sided, MaterialShapes.Triangle, MaterialShapes.Sunny, MaterialShapes.Heart)
    val spokenStep = if (showing && step >= 0) stringResource(R.string.shape_step, step + 1, stringResource(PadNames[sequence[step]])) else null
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Hint(
            when {
                failed && showing -> stringResource(R.string.shapes_wrong)
                showing -> stringResource(R.string.shapes_watch)
                else -> stringResource(R.string.shapes_repeat, position, length)
            },
            error = failed && showing,
            // Read out each step as it lights up; the flashes themselves are visual only.
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (spokenStep != null) contentDescription = spokenStep
            },
        )
        listOf(0, 2).forEach { start ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                (start..start + 1).forEach { pad ->
                    val c = MaterialTheme.colorScheme
                    val label = stringResource(PadNames[pad])
                    val color by animateColorAsState(
                        if (lit == pad) c.primary else c.secondaryContainer,
                        MaterialTheme.motionScheme.fastEffectsSpec(),
                    )
                    Surface(
                        onClick = { tap(pad) },
                        enabled = !showing,
                        shape = pads[pad].toShape(),
                        color = color,
                        modifier = Modifier.size(132.dp).semantics { contentDescription = label },
                    ) {}
                }
            }
        }
    }
}

private val Words = listOf(
    "amber", "river", "stone", "maple", "cloud", "ember", "lunar", "pixel", "orbit", "cedar",
    "delta", "frost", "grove", "harbor", "ivory", "jungle", "kettle", "lemon", "meadow", "noble",
)

@Composable
private fun RetypeChallenge(onSolved: () -> Unit) {
    val target = rememberSaveable {
        buildList { while (joinToString(" ").length < 12) add(Words.random()) }.joinToString(" ")
    }
    var typed by rememberSaveable { mutableStateOf("") }
    val hint = stringResource(R.string.retype_hint)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Hint(hint)
        Text(target, style = MaterialTheme.typography.displaySmallEmphasized, textAlign = TextAlign.Center)
        PillTextField(
            value = typed,
            onValueChange = {
                typed = it
                if (sameWords(it, target)) onSolved()
            },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShakeChallenge(onSolved: () -> Unit) {
    val goal = 30
    var count by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val sensors = remember { context.getSystemService(SensorManager::class.java) }
    val accel = remember { sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    fun shook() {
        if (count >= goal) return
        count++
        if (count == goal) onSolved()
    }

    DisposableEffect(accel) {
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = e.values
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                // ponytail: fixed 2.2 g threshold; tune per device if shakes under- or over-count
                if (g > 2.2f && e.timestamp - last > 250_000_000L) {
                    last = e.timestamp
                    shook()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        accel?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sensors.unregisterListener(listener) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Hint(stringResource(if (accel == null) R.string.shake_unavailable else R.string.shake_hint))
        Surface(onClick = ::shook, enabled = accel == null, shape = MaterialShapes.Circle.toShape(), color = Color.Transparent) {
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { count / goal.toFloat() },
                    modifier = Modifier.size(220.dp),
                    amplitude = if (LocalReduceMotion.current) { _ -> 0f } else { _ -> 1f },
                )
                Text("${goal - count}", style = clockDigits(88.sp))
            }
        }
    }
}
