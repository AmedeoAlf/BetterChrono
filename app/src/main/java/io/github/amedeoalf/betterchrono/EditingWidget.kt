package io.github.amedeoalf.betterchrono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant

@Composable
fun EditingWidget(events: SnapshotStateList<ChronoEvent>, export: () -> Unit, import: () -> Unit) {
    var editingMode by remember { mutableStateOf(false) }
    val toMeasure = remember(events.size) {
        mutableStateListOf(*Array(events.size) { false })
    }
    val selectedEventsIdxs =
        toMeasure.flatMapIndexed { idx, it -> if (it) listOf(idx) else emptyList() }
    val selectedTimestamps = selectedEventsIdxs.map { events[it].instant }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (editingMode) {
                IconButton({ export() }) {
                    Icon(
                        painter = painterResource(R.drawable.outline_upload_24),
                        contentDescription = "Export"
                    )
                }
                IconButton({ import() }) {
                    Icon(
                        painter = painterResource(R.drawable.outline_download_24),
                        contentDescription = "Import"
                    )
                }
                Text(
                    if (selectedTimestamps.size == 2)
                        MsToString[selectedTimestamps[1].millisSince(selectedTimestamps[0])]
                    else "Seleziona due tempi per calcolare la differenza",
                    style = if (selectedTimestamps.size == 2)
                        MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
            if (events.isNotEmpty())
                IconButton({
                    editingMode = !editingMode
                }) {
                    Icon(
                        painter = if (editingMode) painterResource(R.drawable.outline_check_24)
                        else painterResource(R.drawable.outline_edit_24),
                        contentDescription = "Edit mode",
                    )
                }
        }
    }
    if (editingMode) {
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
                            // always keep two checkboxes active at most, remove the oldest checkbox selected
                            if (!toMeasure[idx] && selectedEventsIdxs.size == 2)
                                toMeasure[selectedEventsIdxs[0]] = false
                            toMeasure[idx] = !toMeasure[idx]
                        }
                    )
                    Text(
                        (if (event is StoppedChronoEvent) "STOP: " else "START: ") +
                                MsToString[event.instant.millisSince(events.first().instant)],
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

    val events = remember {
        mutableStateListOf(
            StartChronoEvent(start),
            StoppedChronoEvent(start.plusSeconds(1)),
            StartChronoEvent(start.plusMillis(2023)),
        )
    }

    EditingWidget(events, {}, {})
}
