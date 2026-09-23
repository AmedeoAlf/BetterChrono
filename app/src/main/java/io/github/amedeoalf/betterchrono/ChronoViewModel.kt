package io.github.amedeoalf.betterchrono

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.io.Serializable
import java.time.Instant
import androidx.compose.runtime.State
import java.io.Reader
import java.io.Writer

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
    private var cachedLaps: List<Long> = emptyList()
    private var lastLapsMsEnabledEvents = 0

    private fun computeCurrLap(enabledEvents: List<ChronoEvent>): Long? {
        val lastStart = enabledEvents.indexOfLast { it is StartChronoEvent }
        if (lastStart == -1) return null
        val currTime =
            if (lastStart == enabledEvents.size - 1) currTime.value else enabledEvents[lastStart + 1].instant

        // Find last Start preceded by a Start
        // All Stops must be subtracted from final time
        val iter = enabledEvents.listIterator(lastStart + 1)
        var futureEvent = iter.previous()
        var currentLap: Long? = null
        var pausedTime = 0L
        var nextStart = enabledEvents[lastStart].instant
        while (iter.hasPrevious()) {
            val ev = iter.previous()
            if (ev is StartChronoEvent) {
                when (futureEvent) {
                    is StartChronoEvent -> {
                        currentLap =
                            currTime.millisSince(futureEvent.instant) - pausedTime
                        break
                    }

                    is StoppedChronoEvent -> pausedTime += nextStart.millisSince(
                        futureEvent.instant
                    )
                }
                nextStart = ev.instant
            }
            futureEvent = ev
        }
        return currentLap
    }

    val displayInfo: DisplayInfo
        get() {
            val enabledEvents = events.filter { !it.disabled }
            if (enabledEvents.size >= 2 && enabledEvents.size == lastLapsMsEnabledEvents) {
                val currLap = computeCurrLap(enabledEvents)
                if (currLap != null) return DisplayInfo(
                    cachedLaps.sum() + currLap,
                    cachedLaps,
                    currLap,
                    enabledEvents.last() is StoppedChronoEvent
                )
            }

            val laps = mutableListOf<Long>()
            var lastTime: Instant? = null
            var lapStart: Instant? = null
            var paused = true
            var pausedTime = 0L
            var totalTime = 0L
            for (e in enabledEvents) {
                // lastTime is null only before the first StartChronoEvent, so no time has elapsed
                val elapsed = if (lastTime != null) e.instant.millisSince(lastTime) else 0L
                when (e) {
                    is StartChronoEvent -> {
                        if (paused) {
                            if (lapStart == null) lapStart = e.instant
                            pausedTime += elapsed
                            paused = false
                        } else {
                            // lapStart is null only before the first StartChronoEvent (which occurs when paused is true)
                            val lapDuration = e.instant.millisSince(lapStart!!) - pausedTime
                            laps += lapDuration
                            totalTime += lapDuration
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
            val currLap =
                if (lapStart != null)
                // lastTime is null only before the first StartChronoEvent, should be set on the
                // iteration lapStart is initialized.
                    if (paused) lastTime!!.millisSince(lapStart) - pausedTime
                    else currTime.value.millisSince(lapStart) - pausedTime
                else 0
            cachedLaps = laps
            lastLapsMsEnabledEvents = enabledEvents.size
            return DisplayInfo(currTime = totalTime + currLap, laps, currLap, paused)
        }

    fun export(to: Writer) {
        try {
            val startTime = events.first().instant
            for (e in events) {
                val elapsed = e.instant.millisSince(startTime)
                to.write(MsToString.convert(elapsed, false))
                to.write(" ")
                to.write(if (e is StartChronoEvent) "START" else "STOP")
                if (e.disabled) to.write(" OFF")
                to.write("\n")
            }
        } catch (_: NoSuchElementException) {
        }
    }

    fun import(from: Reader) {
        val imported = from.readLines().map { it.split(" ") }.map {
            val instant = Instant.ofEpochMilli(0).plusMillis(MsToString.parse(it[0]))
            val disabled = it.getOrNull(2) == "OFF"
            when (it[1]) {
                "START" -> StartChronoEvent(instant, disabled)
                "STOP" -> StoppedChronoEvent(instant, disabled)
                else -> throw Exception("'${it[1]}' in savefile must be START or STOP, could not parse")
            }
        }
        this.events.clear()
        this.events.addAll(imported)
    }
}

data class DisplayInfo(
    val currTime: Long,
    val laps: List<Long>,
    val currLap: Long,
    val paused: Boolean
)

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