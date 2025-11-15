package no.nav.helse.spesialist.application

import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringDto

class InMemoryOverstyringApiDao : OverstyringApiDao {
    override fun finnOverstyringer(fødselsnummer: String): List<OverstyringDto> {
        TODO("Not yet implemented")
    }
}
