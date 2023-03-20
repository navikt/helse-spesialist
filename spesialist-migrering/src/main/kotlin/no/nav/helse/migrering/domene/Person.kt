package no.nav.helse.migrering.domene

internal class Person(
    private val aktørId: String,
    private val fødselsnummer: String
) {

    private val observers = mutableSetOf<IPersonObserver>()
    private val arbeidsgivere = mutableListOf<Arbeidsgiver>()
    internal fun register(observer: IPersonObserver) {
        observers.add(observer)
    }

    internal fun opprett() {
        observers.forEach { it.personOpprettet( aktørId, fødselsnummer) }
    }

    internal fun håndterNyArbeidsgiver(organisasjonsnummer: String) {
        val arbeidsgiver = Arbeidsgiver(organisasjonsnummer)
        arbeidsgivere.add(arbeidsgiver)
        arbeidsgiver.register(observer = observers.toTypedArray())
        arbeidsgiver.opprett()
    }
}

internal interface IPersonObserver{

    fun personOpprettet(aktørId: String, fødselsnummer: String) {}

    fun arbeidsgiverOpprettet(organisasjonsnummer: String) {}
}