package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer

interface PersonPseudoIdDao {
    fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId

    fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer?
}
