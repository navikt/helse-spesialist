package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.PersonId

class InMemoryPersonRepository : PersonRepository, AbstractInMemoryRepository<PersonId, Person>() {
    override fun generateId(): PersonId = PersonId((alle().maxOfOrNull { it.id().value } ?: 0) + 1)
}
