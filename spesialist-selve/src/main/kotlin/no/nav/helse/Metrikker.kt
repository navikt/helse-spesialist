package no.nav.helse

import io.prometheus.client.Counter

internal val overstyringsteller = Counter.build("overstyringer", "Teller antall overstyringer")
    .labelNames("opplysningstype", "type")
    .register()

internal val annulleringsteller = Counter.build("annulleringer", "Teller antall annulleringer")
    .register()

internal val automatiseringsteller = Counter.build("automatiseringer", "Teller antall automatiseringer")
    .register()

internal val automatiskAvvistÅrsakerTeller =
    Counter.build("automatisk_avvist_aarsaker", "Årsaker til at en vedtaksperiode avvises automatisk. En vedtaksperiode kan avvises av flere årsaker")
        .labelNames("aarsak")
        .register()

private val warningteller = Counter.build("aktivitet_totals", "Teller antall warnings opprettet i Spesialist")
    .labelNames("alvorlighetsgrad", "melding")
    .register()

private val inaktiveWarningteller = Counter.build("inaktive_warning_totals", "Teller antall warnings satt inaktive i Spesialist")
    .labelNames("alvorlighetsgrad", "melding")
    .register()

internal fun tellWarning(warning: String) = warningteller.labels("WARN", warning).inc()

internal fun tellWarningInaktiv(warning: String) = inaktiveWarningteller.labels("WARN", warning).inc()

