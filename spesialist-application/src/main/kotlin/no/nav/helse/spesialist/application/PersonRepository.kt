package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person

interface PersonRepository {
    fun finn(id: Identitetsnummer): Person?

    fun finnAlle(ider: Set<Identitetsnummer>): List<Person>
}
