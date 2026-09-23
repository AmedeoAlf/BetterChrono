package io.github.amedeoalf.betterchrono

import android.os.Bundle
import android.view.Choreographer
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.amedeoalf.betterchrono.ui.theme.BetterChronoTheme
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    private val vm by viewModels<ChronoViewModel>()
    private var pauseUpdates: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val sm = SavesManager(filesDir.resolve("runs_v0").apply {
            mkdirs()
            if (!exists()) throw Exception("Could not create save directory")
        })
        setContent {
            BetterChronoTheme {
                Screen(vm, sm)
            }
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

    override fun onResume() {
        super.onResume()
        pauseUpdates = runOnEveryFrame {
            vm.updateCurrTime()
        }
    }

    override fun onPause() {
        super.onPause()
        pauseUpdates?.invoke()
        pauseUpdates = null
    }
}

data class Reference<T>(var curr: T)

fun runOnEveryFrame(
    cb: Reference<Choreographer.FrameCallback?> = Reference(null),
    fn: () -> Unit,
): () -> Unit {
    if (cb.curr == null) cb.curr = Choreographer.FrameCallback {
        fn()
        runOnEveryFrame(cb, fn)
    }
    val choreographer = Choreographer.getInstance()
    choreographer.postFrameCallback(cb.curr)
    return { choreographer.removeFrameCallback(cb.curr) }
}

@Composable
fun TimeDisplay(timeMs: Long, isPaused: Boolean) {
    val baseStyle = MaterialTheme.typography.displayLarge
    val baseFontSize = MaterialTheme.typography.displayLarge.fontSize
    var resizedStyle by remember { mutableStateOf(baseStyle.copy(fontSize = baseFontSize * 2)) }
    var shouldDraw by remember { mutableStateOf(false) }
    Text(
        MsToString.convert(timeMs, isPaused),
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
fun Screen(
    vm: ChronoViewModel = viewModel(),
    savesManager: SavesManager = SavesManager(File(""))
) {
    val events = remember { vm.events }
    val showSaveDialog = remember { mutableStateOf(false) }
    val showLoadDialog = remember { mutableStateOf(false) }
    Surface(
        Modifier
            .fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val displayInfo = vm.displayInfo
                ButtonBar(vm)
                TimeDisplay(displayInfo.currTime, displayInfo.paused)
                Column(
                    Modifier
                        .padding(
                            WindowInsets.safeContent.only(WindowInsetsSides.Horizontal)
                                .asPaddingValues()
                        ),
                ) {
                    EditingWidget(
                        events,
                        { showSaveDialog.value = true },
                        { showLoadDialog.value = true })
                    LapDisplay(displayInfo.laps, displayInfo.currLap)
                }
            }
            SaveDialog(showSaveDialog) { savesManager.save(vm, it.toString()) }
            LoadDialog(
                showLoadDialog,
                remember(showLoadDialog.value) { savesManager.listSaves().toList() }
            ) { savesManager.loadSave(vm, it) }
        }
    }
}

fun Instant.millisSince(other: Instant) = ChronoUnit.MILLIS.between(other, this)

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
            .padding(top = 5.dp),
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

