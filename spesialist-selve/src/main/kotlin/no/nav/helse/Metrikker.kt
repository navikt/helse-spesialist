package no.nav.helse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.mediator.GodkjenningsbehovUtfall

internal val registry2 = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
internal val registry = Metrics.globalRegistry.add(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
internal val automatiseringsteller =
    Counter.builder("automatiseringer").description("Teller antall automatiseringer").register(registry)

internal val automatiskAvvistÅrsakerTeller =
    Counter.builder(
        "automatisk_avvist_aarsaker",
    ).description("Årsaker til at en vedtaksperiode avvises automatisk. En vedtaksperiode kan avvises av flere årsaker")
        .register(registry)

private val registrerTidsbrukForHendelse =
    DistributionSummary.builder("command_tidsbruk").description("Måler hvor lang tid en command bruker på å kjøre i ms")

// private val godkjenningsbehovUtfall =
//    Histogram.build("godkjenningsbehov_utfall", "Måler hvor raskt godkjenningsbehov behandles, fordelt på utfallet")
//        .buckets(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 3500.0, 4000.0, 4500.0, 5000.0, 6000.0, 10_000.0, 30_000.0)
//        .labelNames("utfall")
//        .register(registry)
//
// internal val registrerTidsbrukForBehov =
//    Summary.build("behov_tidsbruk", "Måler hvor lang tid et behov tok å løse i ms")
//        .labelNames("behov")
//        .register(registry)
//
// internal val duplikatsjekkTidsbruk =
//    Summary.build("duplikatsjekk_tidsbruk", "Hvor lang tid det tar å sjekke om en melding allerede er behandlet, i ms")
//        .labelNames("var_duplikat")
//        .register(registry)
//
internal fun registrerTidsbrukForHendelse(
    command: String,
    tidBruktMs: Int,
) = registrerTidsbrukForHendelse.tags("command", command).register(registry).record(
    tidBruktMs.toDouble(),
)

internal fun registrerTidsbrukForGodkjenningsbehov(
    utfall: GodkjenningsbehovUtfall,
    tidBruktMs: Int,
) = Unit
