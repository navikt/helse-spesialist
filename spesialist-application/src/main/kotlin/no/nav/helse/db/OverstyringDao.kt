package no.nav.helse.db

import no.nav.helse.spesialist.api.overstyring.OverstyringDto

interface OverstyringDao {
    fun finnAlleForPerson(fødselsnummer: String): List<OverstyringDto>
}
