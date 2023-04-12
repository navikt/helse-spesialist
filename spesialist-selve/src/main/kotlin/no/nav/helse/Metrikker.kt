package no.nav.helse

import io.prometheus.client.Counter
import io.prometheus.client.Summary

internal val overstyringsteller = Counter.build("overstyringer", "Teller antall overstyringer")
    .labelNames("opplysningstype", "type")
    .register()

internal val automatiseringsteller = Counter.build("automatiseringer", "Teller antall automatiseringer")
    .register()

internal val automatiskAvvistÅrsakerTeller =
    Counter.build("automatisk_avvist_aarsaker", "Årsaker til at en vedtaksperiode avvises automatisk. En vedtaksperiode kan avvises av flere årsaker")
        .labelNames("aarsak")
        .register()

private val varselteller = Counter.build("aktivitet_totals", "Teller antall warnings opprettet i Spesialist")
    .labelNames("alvorlighetsgrad", "melding")
    .register()

private val inaktiveVarslerteller = Counter.build("inaktive_warning_totals", "Teller antall warnings satt inaktive i Spesialist")
    .labelNames("alvorlighetsgrad", "melding")
    .register()

private val registrerTidsbrukForHendelse = Summary.build("command_tidsbruk", "Måler hvor lang tid en command bruker på å kjøre i ms")
    .labelNames("command")
    .register()

internal fun tellWarning(warning: String) = varselteller.labels("WARN", warning).inc()

internal fun tellVarsel(varselkode: String) = varselteller.labels("WARN", varselkode).inc()

internal fun tellWarningInaktiv(warning: String) = inaktiveVarslerteller.labels("WARN", warning).inc()

internal fun tellInaktivtVarsel(varselkode: String) = inaktiveVarslerteller.labels("WARN", varselkode).inc()

internal fun registrerTidsbrukForHendelse(command: String, tid: Long) = registrerTidsbrukForHendelse.labels(command).observe(tid.toDouble())
