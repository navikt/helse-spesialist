package no.nav.helse.migrering.domene

import java.util.UUID
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

    internal fun opprett(vedtakSomMangler: List<UUID>) {
        if (vedtaksperioder.all(Vedtaksperiode::erForkastet) && vedtaksperioder.none { it.finnesI(vedtakSomMangler) })
            sikkerlogg.info(
                "Oppretter ikke arbeidsgiver med {} da den ikke har noen ikke-forkastede vedtaksperioder",
                kv("organisasjonsnummer", organisasjonsnummer)
            )
        else {
            observers.forEach { it.arbeidsgiverOpprettet(organisasjonsnummer) }
            vedtaksperioder.forEach { it.opprett(vedtakSomMangler) }
        }
        vedtaksperioder.forEach(Vedtaksperiode::oppdaterForkastet)
    }

    fun håndterNyVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder.add(vedtaksperiode)
        vedtaksperiode.register(observer = observers.toTypedArray())
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        internal fun List<Arbeidsgiver>.harKunForkastedeVedtaksperioder() =
            all { arbeidsgiver ->
                arbeidsgiver.vedtaksperioder.all(Vedtaksperiode::erForkastet)
            }
    }
}
