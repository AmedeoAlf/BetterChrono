package io.github.amedeoalf.betterchrono

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.io.Serializable
import java.time.Instant
import androidx.compose.runtime.State

class ChronoViewModel : Serializable, ViewModel() {
    val events = mutableStateListOf<ChronoEvent>()
    private val _currTime = mutableStateOf(Instant.now())

    val currTime: State<Instant>
        get() = _currTime

    fun updateCurrTime() {
        _currTime.value = Instant.now()
    }

    fun addStartEvent() = events.add(StartChronoEvent(currTime.value))
    fun addStopEvent() = events.add(StoppedChronoEvent(currTime.value))

    val displayMs: Long
        get() {
            var time = 0L
            var lastStart: Instant? = null
            for (e in events.filter { !it.disabled })
                when (e) {
                    is StartChronoEvent -> lastStart = lastStart ?: e.instant
                    is StoppedChronoEvent -> if (lastStart != null) {
                        time += e.instant.millisSince(lastStart)
                        lastStart = null
                    }
                }

            if (lastStart != null)
                time += currTime.value.millisSince(lastStart)
            return time
        }

    val lapsMs: List<Long>
        get() {
            val laps = mutableListOf<Long>()
            var lastTime: Instant? = null
            // lapStart is null only before the first StartChronoEvent (which occurs when paused is true)
            var lapStart: Instant? = null
            var paused = true
            var pausedTime = 0L
            for (e in events.filter { !it.disabled }) {
                val elapsed = if (lastTime != null) e.instant.millisSince(lastTime) else 0L
                when (e) {
                    is StartChronoEvent -> {
                        if (paused) {
                            if (lapStart == null) lapStart = e.instant
                            pausedTime += elapsed
                            paused = false
                        } else {
                            laps += e.instant.millisSince(lapStart!!) - pausedTime
                            pausedTime = 0L
                            lapStart = e.instant
                        }
                    }

                    is StoppedChronoEvent -> {
                        // ignore event, do not update lastTime
                        if (paused) continue
                        paused = true
                    }
                }
                lastTime = e.instant
            }
            return laps
        }
}

abstract class ChronoEvent(val instant: Instant, val disabled: Boolean = false) : Serializable {
    abstract fun withDisabled(value: Boolean): ChronoEvent
}

class StartChronoEvent(instant: Instant, disabled: Boolean = false) :
    ChronoEvent(instant, disabled) {
    override fun withDisabled(value: Boolean): StartChronoEvent = StartChronoEvent(instant, value)
}

class StoppedChronoEvent(instant: Instant, disabled: Boolean = false) :
    ChronoEvent(instant, disabled) {
    override fun withDisabled(value: Boolean): StoppedChronoEvent =
        StoppedChronoEvent(instant, value)
}