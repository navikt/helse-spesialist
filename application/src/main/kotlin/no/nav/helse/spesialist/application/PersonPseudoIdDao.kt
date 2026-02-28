package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.Duration

interface PersonPseudoIdDao {
    fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId

    fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer?

    fun slettPseudoIderEldreEnn(alder: Duration): Int
}
