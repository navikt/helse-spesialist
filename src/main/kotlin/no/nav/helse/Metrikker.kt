package no.nav.helse

import io.prometheus.client.Collector.NANOSECONDS_PER_SECOND
import io.prometheus.client.Histogram

private val histogram =
    Histogram.build("timer", "Dette er en generell timer")
        .labelNames("measurement", "duration")
        .register()

internal fun <T> measureAsHistogram(measurement: String, block: () -> T): T {
    val result: T
    val start = System.nanoTime()
    try {
        result = block()
    } catch (e: Throwable) {
        histogram.labels(measurement, "failure")
            .observe((System.nanoTime() - start) / NANOSECONDS_PER_SECOND)
        throw e
    }
    histogram.labels(measurement, "success").observe((System.nanoTime() - start) / NANOSECONDS_PER_SECOND)
    return result
}
