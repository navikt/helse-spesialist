package no.nav.helse

import io.micrometer.core.instrument.Counter
import no.nav.helse.mediator.GodkjenningsbehovUtfall

internal val automatiseringsteller =
    Counter.builder("automatiseringer").description("Teller antall automatiseringer").register()

internal val automatiskAvvistÅrsakerTeller =
    Counter.build(
        "automatisk_avvist_aarsaker",
        "Årsaker til at en vedtaksperiode avvises automatisk. En vedtaksperiode kan avvises av flere årsaker",
    )
        .labelNames("aarsak")
        .register()

private val registrerTidsbrukForHendelse =
    Summary.build("command_tidsbruk", "Måler hvor lang tid en command bruker på å kjøre i ms")
        .labelNames("command")
        .register()

private val godkjenningsbehovUtfall =
    Histogram.build("godkjenningsbehov_utfall", "Måler hvor raskt godkjenningsbehov behandles, fordelt på utfallet")
        .buckets(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 3500.0, 4000.0, 4500.0, 5000.0, 6000.0, 10_000.0, 30_000.0)
        .labelNames("utfall")
        .register()

internal val registrerTidsbrukForBehov =
    Summary.build("behov_tidsbruk", "Måler hvor lang tid et behov tok å løse i ms")
        .labelNames("behov")
        .register()

internal val duplikatsjekkTidsbruk =
    Summary.build("duplikatsjekk_tidsbruk", "Hvor lang tid det tar å sjekke om en melding allerede er behandlet, i ms")
        .labelNames("var_duplikat")
        .register()

internal fun registrerTidsbrukForHendelse(
    command: String,
    tidBruktMs: Int,
) = registrerTidsbrukForHendelse.labels(
    command,
).observe(tidBruktMs.toDouble())

internal fun registrerTidsbrukForGodkjenningsbehov(
    utfall: GodkjenningsbehovUtfall,
    tidBruktMs: Int,
) = godkjenningsbehovUtfall.labels(utfall.name).observe(tidBruktMs.toDouble())
