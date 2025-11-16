package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.PersonId

interface PersonRepository {
    fun finn(id: PersonId): Person?

    fun finn(identitetsnummer: Identitetsnummer): Person?

    fun finnAlle(ider: Set<PersonId>): List<Person>
}
