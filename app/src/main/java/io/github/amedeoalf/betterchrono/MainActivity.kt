package io.github.amedeoalf.betterchrono

import androidx.compose.material.icons.Icons
import android.os.Bundle
import android.view.Choreographer
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.amedeoalf.betterchrono.ui.theme.BetterChronoTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    private val vm by viewModels<ChronoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterChronoTheme {
                Screen(vm)
            }
        }
        runOnEveryFrame {
            vm.updateCurrTime()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) =
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event?.repeatCount == 0)
                    vm.addStopEvent()
                true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event?.repeatCount == 0)
                    vm.addStartEvent()
                true
            }

            else -> super.onKeyDown(keyCode, event)
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
fun Screen(vm: ChronoViewModel = viewModel()) {
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
            Column(
                Modifier.safeContentPadding(),
                horizontalAlignment = Alignment.End
            ) {
                IconButton({
                    editingMode = !editingMode
                }) {
                    Icon(
                        if (editingMode) rememberVectorPainter(image = Icons.Rounded.Done)
                        else rememberVectorPainter(image = Icons.Rounded.Edit),
                        contentDescription = "Edit mode",
                    )
                }
                if (editingMode) {
                    EditingWidget(events)
                } else {
                    LapDisplay(vm.lapsMs)
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
    val now = Instant.now()

    val vm: ChronoViewModel = viewModel()
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
    Row(
        Modifier
            .fillMaxWidth()
            .safeGesturesPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
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