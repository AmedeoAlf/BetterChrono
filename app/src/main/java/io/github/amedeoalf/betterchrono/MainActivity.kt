package io.github.amedeoalf.betterchrono

import android.os.Build
import android.os.Bundle
import android.view.Choreographer
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.util.fastMap
import io.github.amedeoalf.betterchrono.ui.theme.BetterChronoTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    var viewModel = ChronoViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable("viewModel", ChronoViewModel::class.java)
            } else {
                it.getSerializable("viewModel") as ChronoViewModel?
            }
        }?.let {
            viewModel = it
        }
        enableEdgeToEdge()
        setContent {
            BetterChronoTheme {
                Screen(viewModel)
            }
        }
        runOnEveryFrame {
            viewModel.updateCurrTime()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) =
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event?.repeatCount == 0)
                    viewModel.addStopEvent()
                true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event?.repeatCount == 0)
                    viewModel.addStartEvent()
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("viewModel", viewModel)
    }
}

fun runOnEveryFrame(fn: () -> Unit): Unit =
    Choreographer.getInstance().postFrameCallback {
        fn()
        runOnEveryFrame(fn)
    }

@Composable
fun TimeDisplay(timeMs: Long) {
    val baseStyle = MaterialTheme.typography.displayLarge
    val baseFontSize = MaterialTheme.typography.displayLarge.fontSize
    var resizedStyle by remember { mutableStateOf(baseStyle.copy(fontSize = baseFontSize * 2)) }
    var shouldDraw by remember { mutableStateOf(false) }
    Text(
        timeMs.toMillisString(),
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                if (shouldDraw) drawContent()
            },
        style = resizedStyle,
        softWrap = false,
        textAlign = TextAlign.Center,
        onTextLayout = {
            if (it.didOverflowWidth) {
                shouldDraw = false
                if (resizedStyle.fontSize.isUnspecified) {
                    resizedStyle = resizedStyle.copy(fontSize = baseFontSize)
                }
                resizedStyle = resizedStyle.copy(fontSize = resizedStyle.fontSize * 0.9)
            } else {
                shouldDraw = true
            }
        }
    )
}

@Composable
fun Screen(vm: ChronoViewModel) {
    var editingMode by remember { mutableStateOf(false) }
    val events = remember { vm.events }
    Surface(
        Modifier
            .fillMaxSize()
    ) {
        Column(
            Modifier.safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ButtonBar(vm)
            TimeDisplay(vm.displayMs)
            Button({ editingMode = !editingMode }) { Text(if (editingMode) "Fine" else "Modifica") }
            if (editingMode) {
                EditingWidget(events)
            } else {
                LapDisplay(vm.lapsMs)
            }
        }
    }
}

fun Instant.millisSince(other: Instant) = ChronoUnit.MILLIS.between(other, this)
fun Long.toMillisString() = "%02d:%02d.%03d".format(
    this / (1000 * 60),
    this / 1000 % 60,
    this % 1000,
)

@Composable
fun EditingWidget(events: MutableList<ChronoEvent>) {
    println("got ${events.size} events")
    var toMeasure by remember {
        println("and i'm reloading things")
        mutableStateOf(events.fastMap { false })
    }
    val selectedEventsIdxs =
        toMeasure.flatMapIndexed { idx, it -> if (it) listOf(idx) else emptyList() }
    val selectedTimestamps = selectedEventsIdxs.map { events[it].instant }

    Column {
        Text(
            if (selectedTimestamps.size == 2)
                selectedTimestamps[1].millisSince(selectedTimestamps[0]).toMillisString()
            else "Seleziona due tempi per calcolare la differenza",
            style = if (selectedTimestamps.size == 2)
                MaterialTheme.typography.titleLarge
            else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            val baseModifier = Modifier.padding(10.dp)
            Text("Misura", modifier = baseModifier)
            Text("Tempo", modifier = baseModifier.weight(1f), textAlign = TextAlign.Center)
            Text("Attiva", modifier = baseModifier)
        }
        LazyColumn {
            itemsIndexed(events) { idx, event ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        toMeasure[idx],
                        {
                            toMeasure = toMeasure.toMutableList().also {
                                // always keep two checkboxes active at most, remove the oldest checkbox selected
                                if (!it[idx] && selectedEventsIdxs.size == 2)
                                    it[selectedEventsIdxs[0]] = false
                                it[idx] = !it[idx]
                            }
                        }
                    )
                    Text(
                        (if (event is StoppedChronoEvent) "STOP: " else "START: ") +
                                event.instant.millisSince(events.first().instant).toMillisString(),
                        Modifier.weight(1f)
                    )
                    Checkbox(
                        !event.disabled,
                        {
                            events[idx] = event.withDisabled(!event.disabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_3_XL)
fun EditingWidgetPreview() {
    val start = Instant.ofEpochMilli(1789549106713)

    var events = remember {
        mutableStateListOf(
            StartChronoEvent(start),
            StoppedChronoEvent(start.plusSeconds(1)),
            StartChronoEvent(start.plusMillis(2023)),
        )
    }

    EditingWidget(events)
}

@Composable
fun LapDisplay(laps: List<Long>) {
    LazyVerticalGrid(
        GridCells.Adaptive(110.dp),
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(laps) { idx, it ->
            LapEntry(idx, it)
        }
    }
}

@Composable
fun LapEntry(idx: Int, timeMs: Long) {
    Card {
        Column(Modifier.padding(5.dp)) {
            Text(
                "Lap ${idx + 1}",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight(800)),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                timeMs.toMillisString(),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Preview(device = Devices.PIXEL_3_XL, showSystemUi = true)
@Composable
fun ScreenPreview() {
    val now = Instant.now()

    val vm = ChronoViewModel()
    vm.events.addAll(
        listOf(
            StartChronoEvent(now.minusSeconds(4)),
            StartChronoEvent(now.minusMillis(3007)),
            StartChronoEvent(now.minusSeconds(2)),
            StartChronoEvent(now.minusMillis(1033)),
        )
    )
    Screen(vm)
}

@Composable
fun ButtonBar(vm: ChronoViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        @Composable
        fun ChronoBtn(
            label: String,
            smallText: String,
            modifier: Modifier = Modifier,
            buttonColors: ButtonColors = ButtonDefaults.buttonColors(),
            onClick: () -> Unit,
        ) =
            Button(onClick, modifier = modifier, colors = buttonColors) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((-7).dp)
                ) {
                    Text(label)
                    Text(smallText, style = MaterialTheme.typography.bodySmall.let {
                        it.copy(fontSize = it.fontSize.times(0.7f))
                    })
                }
            }
        ChronoBtn(
            "Reset",
            "Azzera tutto",
            buttonColors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
        ) { vm.events.clear() }
        ChronoBtn("Ferma", "vol +") { vm.addStopEvent() }
        ChronoBtn("Avvia/Giro", "vol -") { vm.addStartEvent() }
    }

}