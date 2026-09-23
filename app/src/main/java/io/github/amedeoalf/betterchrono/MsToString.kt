package io.github.amedeoalf.betterchrono

object MsToString {
    private val cache = mutableMapOf<Long, String>()

    operator fun get(millis: Long) = convert(millis)
    fun convert(millis: Long, shouldCache: Boolean = true): String =
        if (cache.contains(millis)) cache[millis]!!
        else if (shouldCache) {
            convert(millis, false).also { cache[millis] = it }
        } else
            "%02d:%02d.%03d".format(
                (millis / (1000 * 60)).coerceAtMost(99),
                millis / 1000 % 60,
                millis % 1000,
            )

    fun parse(str: String) =
        str.substring(0..1).toLong() * 1000 * 60 +
                str.substring(3..4).toLong() * 1000 +
                str.substring(6..8).toLong()
}