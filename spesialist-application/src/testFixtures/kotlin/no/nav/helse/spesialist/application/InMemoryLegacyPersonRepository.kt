package no.nav.helse.spesialist.application

import no.nav.helse.modell.person.LegacyPersonRepository
import no.nav.helse.modell.person.Person
import no.nav.helse.spesialist.application.logg.logg

class InMemoryLegacyPersonRepository : LegacyPersonRepository {
    private val personer: MutableList<Person> = mutableListOf()

    fun leggTilPerson(person: Person) {
        personer.add(person)
    }

    override fun brukPersonHvisFinnes(fødselsnummer: String, personScope: Person.() -> Unit) {
        personer.firstOrNull { it.fødselsnummer == fødselsnummer }?.personScope()
            ?: logg.info("Person med fødselsnummer $fødselsnummer er ikke lagt til i testen")
    }

    override fun finnFødselsnumre(aktørId: String) =
        personer.filter { it.aktørId == aktørId }.map { it.fødselsnummer }
}
