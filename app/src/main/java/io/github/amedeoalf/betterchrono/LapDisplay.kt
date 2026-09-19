package io.github.amedeoalf.betterchrono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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

