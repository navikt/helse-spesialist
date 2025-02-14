package no.nav.helse.db.api

import no.nav.helse.spesialist.api.overstyring.OverstyringDto

interface OverstyringApiDao {
    fun finnOverstyringer(fødselsnummer: String): List<OverstyringDto>
}
