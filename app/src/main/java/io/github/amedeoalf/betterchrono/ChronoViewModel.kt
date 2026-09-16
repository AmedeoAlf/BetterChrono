package io.github.amedeoalf.betterchrono

import java.io.Serializable
import java.time.Instant

data class ChronoViewModel(val events: List<ChronoEvent>, val currTime: Instant) : Serializable {
    fun withStartEvent() = copy(events = events + StartChronoEvent(currTime))

    // if already stopped, don't add a stopped event
    fun withStopEvent() = if (events.lastOrNull() is StoppedChronoEvent) this
    else copy(
        events = events + StoppedChronoEvent(currTime)
    )

    val displayMs: Long
        get() {
            var time = 0L
            var lastStart: Instant? = null
            for (e in events)
                when (e) {
                    is StartChronoEvent -> lastStart = lastStart ?: e.instant
                    is StoppedChronoEvent -> if (lastStart != null) {
                        time += e.instant.millisSince(lastStart)
                        lastStart = null
                    }
                }

            if (lastStart != null)
                time += currTime.millisSince(lastStart)
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
            for (e in events) {
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

abstract class ChronoEvent(val instant: Instant) : Serializable
class StartChronoEvent(instant: Instant) : ChronoEvent(instant)
class StoppedChronoEvent(instant: Instant) : ChronoEvent(instant)