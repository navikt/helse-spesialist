package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.PersonId

class InMemoryPersonRepository : PersonRepository, AbstractInMemoryRepository<PersonId, Person>() {
    override fun tildelIder(root: Person) {
        if (!root.harFåttTildeltId())
            root.tildelId(PersonId((alle().maxOfOrNull { it.id().value } ?: 0) + 1))
    }

    override fun deepCopy(original: Person): Person = Person.Factory.fraLagring(
        id = original.id(),
        identitetsnummer = original.identitetsnummer,
        aktørId = original.aktørId,
        info = original.info,
        infoOppdatert = original.infoOppdatert,
        enhetRef = original.enhetRef,
        enhetRefOppdatert = original.enhetRefOppdatert,
        infotrygdutbetalingerRef = original.infotrygdutbetalingerRef,
        infotrygdutbetalingerOppdatert = original.infotrygdutbetalingerOppdatert,
    )
}
