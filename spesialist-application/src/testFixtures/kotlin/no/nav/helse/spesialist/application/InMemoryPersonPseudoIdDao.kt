package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.Duration
import java.time.Instant

class InMemoryPersonPseudoIdDao : PersonPseudoIdDao {
    private val mapping = mutableMapOf<PersonPseudoId, Pair<Identitetsnummer, Instant>>()
    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId {
        val personPseudoId = PersonPseudoId.ny()
        mapping[personPseudoId] = identitetsnummer to Instant.now()
        return personPseudoId
    }

    override fun hentIdentitetsnummer(personPseudoId: PersonPseudoId) = mapping[personPseudoId]?.first
    override fun slettPseudoIderEldreEnn(alder: Duration): Int {
        val antallFørSletting = mapping.size
        mapping.entries.removeIf {
            it.value.second < Instant.now().minus(alder)
        }
        return antallFørSletting - mapping.size
    }
}
