package no.nav.helse.migrering.domene

internal class Arbeidsgiver(
    private val organisasjonsnummer: String
) {
    private val observers = mutableSetOf<IPersonObserver>()

    internal fun register(vararg observer: IPersonObserver) {
        observers.addAll(observer)
    }

    internal fun opprett() {
        observers.forEach { it.arbeidsgiverOpprettet(organisasjonsnummer) }
    }
}
