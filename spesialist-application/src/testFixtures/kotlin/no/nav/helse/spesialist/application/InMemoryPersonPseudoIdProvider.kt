package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.Instant

class InMemoryPersonPseudoIdProvider : PersonPseudoIdProvider {
    private val mapping = mutableMapOf<PersonPseudoId, Pair<Identitetsnummer, Instant>>()

    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId {
        val personPseudoId = PersonPseudoId.ny()
        mapping[personPseudoId] = identitetsnummer to Instant.now()
        return personPseudoId
    }

    override fun hentIdentitetsnummer(personPseudoId: PersonPseudoId) = mapping[personPseudoId]?.first
}
