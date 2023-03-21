package no.nav.helse.migrering.domene

internal class Arbeidsgiver(
    private val organisasjonsnummer: String
) {
    private val observers = mutableSetOf<IPersonObserver>()
    private val vedtaksperioder = mutableListOf<Vedtaksperiode>()

    internal fun register(vararg observer: IPersonObserver) {
        observers.addAll(observer)
    }

    internal fun opprett() {
        observers.forEach { it.arbeidsgiverOpprettet(organisasjonsnummer) }
    }

    fun håndterNyVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder.add(vedtaksperiode)
        vedtaksperiode.register(observer = observers.toTypedArray())
        vedtaksperiode.opprett()
    }
}
