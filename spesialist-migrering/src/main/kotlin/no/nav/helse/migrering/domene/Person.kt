package no.nav.helse.migrering.domene

internal class Person(
    private val aktørId: String,
    private val fødselsnummer: String
) {

    private val observers = mutableSetOf<IPersonObserver>()
    internal fun register(observer: IPersonObserver) {
        observers.add(observer)
    }

    internal fun opprett() {
        observers.forEach { it.personOpprettet( aktørId, fødselsnummer) }
    }
}

internal interface IPersonObserver{

    fun personOpprettet(aktørId: String, fødselsnummer: String) {}
}