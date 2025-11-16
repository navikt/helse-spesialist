package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person

class InMemoryPersonRepository : PersonRepository, AbstractInMemoryRepository<Identitetsnummer, Person>() {
    override fun deepCopy(original: Person): Person = Person.Factory.fraLagring(
        id = original.id,
        aktørId = original.aktørId,
        info = original.info,
        infoOppdatert = original.infoOppdatert,
        enhetRef = original.enhetRef,
        enhetRefOppdatert = original.enhetRefOppdatert,
        infotrygdutbetalingerRef = original.infotrygdutbetalingerRef,
        infotrygdutbetalingerOppdatert = original.infotrygdutbetalingerOppdatert,
        egenAnsattStatus = original.egenAnsattStatus,
    )
}
