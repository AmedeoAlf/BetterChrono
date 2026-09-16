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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import io.github.amedeoalf.betterchrono.ui.theme.BetterChronoTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    val viewModel = mutableStateOf(ChronoViewModel(emptyList(), Instant.now()))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable("viewModel", ChronoViewModel::class.java)
            } else {
                it.getSerializable("viewModel") as ChronoViewModel?
            }
        }?.let {
            viewModel.value = it
        }
        enableEdgeToEdge()
        setContent {
            BetterChronoTheme {
                Screen(viewModel.value) { viewModel.value = it }
            }
        }
        runOnEveryFrame {
            viewModel.value = viewModel.value.copy(currTime = Instant.now())
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) =
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                viewModel.value = viewModel.value.withStopEvent()
                true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                viewModel.value = viewModel.value.withStartEvent()
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("viewModel", viewModel.value)
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
    var resizedStyle by remember { mutableStateOf(baseStyle.copy(fontSize = baseFontSize * 10)) }
    var shouldDraw by remember { mutableStateOf(false) }
    Text(
        timeMs.toMillisString(),
        modifier = Modifier.drawWithContent {
                if (shouldDraw) drawContent()
            },
        style = resizedStyle,
        softWrap = false,
        onTextLayout = {
            if (it.didOverflowWidth) {
                shouldDraw = false
                if (resizedStyle.fontSize.isUnspecified) {
                    resizedStyle = resizedStyle.copy(fontSize = baseFontSize)
                }
                resizedStyle = resizedStyle.copy(fontSize = baseStyle.fontSize * 0.95)
            } else {
                shouldDraw = true
            }
        }
    )
}

@Composable
fun Screen(vm: ChronoViewModel, updateVm: (ChronoViewModel) -> Unit) {
    Surface(
        Modifier
            .fillMaxSize()
    ) {
        Column(Modifier.safeDrawingPadding()) {
            ButtonBar(vm, updateVm)
            TimeDisplay(vm.displayMs)
//            Text(vm.events.joinToString { (if (it is StoppedChronoEvent) "STOP:" else "") + it.instant.millisSince(vm.events[0].instant).toMillisString() })
            LazyColumn(Modifier.fillMaxWidth()) {
                itemsIndexed(vm.lapsMs) { idx, it ->
                    Text(
                        "${idx + 1}. " +
                                it.toMillisString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
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

@Preview(device = Devices.PIXEL_3_XL, showSystemUi = true)
@Composable
fun ScreenPreview() {
    val start = Instant.ofEpochMilli(1789549106713)

    val vm = ChronoViewModel(
        listOf(
            StartChronoEvent(start),
            StartChronoEvent(start.plusSeconds(1)),
            StartChronoEvent(start.plusMillis(2023)),
        ),
        start.plusMillis(3023),
    )
    Screen(vm) {}
}

@Composable
fun ButtonBar(vm: ChronoViewModel, updateVm: (ChronoViewModel) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        @Composable
        fun ChronoBtn(
            label: String,
            smallText: String,
            newViewModel: () -> ChronoViewModel
        ) =
            Button({ updateVm(newViewModel()) }) {
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
        ChronoBtn("Reset", "Azzera tutto") { vm.copy(events = emptyList()) }
        ChronoBtn("Ferma", "vol +") { vm.withStopEvent() }
        ChronoBtn("Avvia/Giro", "vol -") { vm.withStartEvent() }
    }

}