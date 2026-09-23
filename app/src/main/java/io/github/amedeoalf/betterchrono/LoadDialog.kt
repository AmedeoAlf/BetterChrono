package io.github.amedeoalf.betterchrono

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Preview(showSystemUi = true)
@Composable
fun LoadDialog(
    shown: MutableState<Boolean> = mutableStateOf(true),
    fileList: List<File> = emptyList(),
    load: (filename: String) -> Unit = {}
) {
    var shown by shown
    if (shown) Dialog({ shown = false }) {
        Card(
            colors = CardDefaults.cardColors()
                .copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Column(
                Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Carica",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "Carica tempi salvati in precedenza",
                    textAlign = TextAlign.Center
                )
                Text(
                    if (fileList.size == 1) "1 salvataggio" else "${fileList.size} salvataggi",
                    Modifier.align(Alignment.Start)
                )
                val dateTimeFormatter =
                    DateTimeFormatter.ofPattern("kk:mm dd-MM-uu").withZone(ZoneId.systemDefault())
                LazyColumn(
                    Modifier
                        .heightIn(max = 300.dp)
                        .clip(MaterialTheme.shapes.medium),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(fileList) {
                        Box(
                            Modifier
                                .clickable { load(it.name) }
                                .background(MaterialTheme.colorScheme.primaryContainer)) {
                            Row(
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .heightIn(min = 40.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(it.name, modifier = Modifier.weight(1f))
                                Text(dateTimeFormatter.format(Instant.ofEpochMilli(it.lastModified())))
                            }
                        }
                    }
                }
                Button(
                    { shown = false },
                    colors = ButtonDefaults.textButtonColors()
                ) { Text("Chiudi") }
            }
        }
    }
}