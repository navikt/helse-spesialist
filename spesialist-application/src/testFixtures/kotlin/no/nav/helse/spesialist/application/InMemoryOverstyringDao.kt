package no.nav.helse.spesialist.application

import no.nav.helse.db.OverstyringDao
import no.nav.helse.spesialist.api.overstyring.OverstyringDto

class InMemoryOverstyringDao : OverstyringDao {
    val overstyringer = mutableListOf<OverstyringDto>()

    override fun finnAlleForPerson(fødselsnummer: String): List<OverstyringDto> = overstyringer.filter { it.fødselsnummer == fødselsnummer }
}
