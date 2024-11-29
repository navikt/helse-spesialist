package no.nav.helse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.mediator.GodkjenningsbehovUtfall
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag

private val registry = Metrics.globalRegistry.add(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
private val automatiseringsteller =
    Counter
        .builder("automatiseringer")
        .description("Teller antall automatiseringer")
        .register(registry)

private val annulleringsteller =
    Counter.builder("annulleringer")
        .description("Teller antall annulleringer")
        .register(registry)

private val automatiskAvvistÅrsakerTellerBuilder =
    Counter.builder("automatisk_avvist_aarsaker")
        .description("Årsaker til at en vedtaksperiode avvises automatisk. En vedtaksperiode kan avvises av flere årsaker")

private val tidsbrukForHendelseMetrikkBuilder =
    DistributionSummary
        .builder("command_tidsbruk")
        .description("Måler hvor lang tid en command bruker på å kjøre i ms")

private val godkjenningsbehovUtfallMetrikkBuilder =
    DistributionSummary
        .builder("godkjenningsbehov_utfall")
        .description("Måler hvor raskt godkjenningsbehov behandles, fordelt på utfallet")
        .serviceLevelObjectives(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 3500.0, 4000.0, 4500.0, 5000.0, 6000.0, 10_000.0, 30_000.0)

private val tidsbrukForBehovMetrikkBuilder =
    DistributionSummary
        .builder("behov_tidsbruk")
        .description("Måler hvor lang tid et behov tok å løse i ms")

private val duplikatsjekkTidsbrukMetrikkBuilder =
    DistributionSummary
        .builder("duplikatsjekk_tidsbruk")
        .description("Hvor lang tid det tar å sjekke om en melding allerede er behandlet, i ms")

private val overstyringstellerBuilder =
    Counter
        .builder("overstyringer")
        .description("Teller antall overstyringer")
        .tags("opplysningstype", "type")

internal fun registrerTidsbrukForDuplikatsjekk(
    erDuplikat: Boolean,
    tid: Double,
) = duplikatsjekkTidsbrukMetrikkBuilder
    .withRegistry(registry)
    .withTag("var_duplikat", erDuplikat.toString())
    .record(tid)

internal fun registrerTidsbrukForBehov(
    behov: String,
    tid: Double,
) = tidsbrukForBehovMetrikkBuilder
    .withRegistry(registry)
    .withTag("behov", behov)
    .record(tid)

internal fun registrerTidsbrukForHendelse(
    command: String,
    tidBruktMs: Int,
) = tidsbrukForHendelseMetrikkBuilder
    .withRegistry(registry)
    .withTag("command", command)
    .record(tidBruktMs.toDouble())

internal fun registrerTidsbrukForGodkjenningsbehov(
    utfall: GodkjenningsbehovUtfall,
    tidBruktMs: Int,
) = godkjenningsbehovUtfallMetrikkBuilder
    .withRegistry(registry)
    .withTag("utfall", utfall.name)
    .record(tidBruktMs.toDouble())

internal fun tellAvvistÅrsak(årsak: String) =
    automatiskAvvistÅrsakerTellerBuilder
        .withRegistry(registry)
        .withTag("årsak", årsak)
        .increment()

internal fun tellAutomatisering() {
    automatiseringsteller
        .increment()
}

private fun tellAnnullering() =
    annulleringsteller
        .increment()

private fun tellOverstyrTidslinje() =
    overstyringstellerBuilder
        .withRegistry(registry)
        .withTag("opplysningstype", "tidslinje")
        .increment()

private fun tellOverstyrArbeidsforhold() =
    overstyringstellerBuilder
        .withRegistry(registry)
        .withTag("opplysningstype", "arbeidsforhold")
        .increment()

private fun tellOverstyrInntektOgRefusjon() =
    overstyringstellerBuilder
        .withRegistry(registry)
        .withTag("opplysningstype", "inntektogrefusjon")
        .increment()

private fun tellSkjønnsfastsettingSykepengegrunnlag() =
    overstyringstellerBuilder
        .withRegistry(registry)
        .withTag("opplysningstype", "skjønnsfastsettingsykepengegrunnlag")
        .increment()

fun tell(handling: Handling) =
    when (handling) {
        is OverstyrtTidslinje -> tellOverstyrTidslinje()
        is OverstyrtInntektOgRefusjon -> tellOverstyrInntektOgRefusjon()
        is OverstyrtArbeidsforhold -> tellOverstyrArbeidsforhold()
        is SkjønnsfastsattSykepengegrunnlag -> tellSkjønnsfastsettingSykepengegrunnlag()
        is Annullering -> tellAnnullering()
        else -> {}
    }
