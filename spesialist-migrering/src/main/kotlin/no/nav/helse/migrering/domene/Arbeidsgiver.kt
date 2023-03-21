package no.nav.helse.migrering.domene

import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

internal class Arbeidsgiver(
    private val organisasjonsnummer: String
) {
    private val observers = mutableSetOf<IPersonObserver>()
    private val vedtaksperioder = mutableListOf<Vedtaksperiode>()

    internal fun register(vararg observer: IPersonObserver) {
        observers.addAll(observer)
    }

    internal fun opprett() {
        if (vedtaksperioder.all { it.erForkastet() })
            return sikkerlogg.info(
                "Oppretter ikke arbeidsgiver med {} da den ikke har noen ikke-forkastede vedtaksperioder",
                kv("organisasjonsnummer", organisasjonsnummer)
            )
        observers.forEach { it.arbeidsgiverOpprettet(organisasjonsnummer) }
        vedtaksperioder.forEach { it.opprett() }
    }

    fun håndterNyVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder.add(vedtaksperiode)
        vedtaksperiode.register(observer = observers.toTypedArray())
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        internal fun List<Arbeidsgiver>.harAktiveVedtaksperioder() =
            any { arbeidsgiver ->
                arbeidsgiver.vedtaksperioder
                    .filterNot(Vedtaksperiode::erForkastet)
                    .isNotEmpty()
            }
    }
}
