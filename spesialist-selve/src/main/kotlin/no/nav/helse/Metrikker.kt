package no.nav.helse

import io.prometheus.client.Collector.NANOSECONDS_PER_SECOND
import io.prometheus.client.Counter
import io.prometheus.client.Histogram

private val histogram =
    Histogram.build("latency", "Måler hvor lang tid en kodeblokk tar")
        .labelNames("measurement", "duration")
        .register()

internal val overstyringsteller = Counter.build("overstyringer", "Teller antall overstyringer")
    .labelNames("opplysningstype", "type")
    .register()


internal val annulleringsteller = Counter.build("annulleringer", "Teller antall annulleringer")
    .register()

internal val automatiseringsteller = Counter.build("automatiseringer", "Teller antall automatiseringer")
    .register()

internal val avvistPåGrunnAvEgenAnsattTeller =
    Counter.build("avvist_egen_ansatt", "Teller antall avvisninger på grunn av egen ansatt")
        .register()

internal val avvistPåGrunnAvUtlandTeller =
    Counter.build("avvist_utland", "Teller antall avvisninger på grunn av tilhørighet til utlandsenhet")
        .register()

internal val warningteller = Counter.build("aktivitet_totals", "Teller antall warnings opprettet i Spesialist")
    .labelNames("alvorlighetsgrad", "melding")
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
