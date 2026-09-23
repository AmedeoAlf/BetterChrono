package io.github.amedeoalf.betterchrono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Preview(showSystemUi = true)
@Composable
fun SaveDialog(
    shown: MutableState<Boolean> = mutableStateOf(true),
    save: (filename: CharSequence) -> Unit = {}
) {
    var shown by shown
    if (shown) {
        Dialog({ shown = false }) {
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
                        "Salva",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Recupera questi tempi in un secondo momento",
                        textAlign = TextAlign.Center
                    )
                    val tf = rememberTextFieldState()
                    TextField(
                        tf,
                        label = { Text("Nome salvataggio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            { shown = false },
                            colors = ButtonDefaults.textButtonColors()
                        ) { Text("Annulla") }
                        Button(
                            {
                                save(tf.text)
                                shown = false
                            },
                            enabled = tf.text.isNotEmpty(),
                            colors = ButtonDefaults.textButtonColors()
                        ) { Text("Salva") }
                    }
                }
            }
        }
    }

}