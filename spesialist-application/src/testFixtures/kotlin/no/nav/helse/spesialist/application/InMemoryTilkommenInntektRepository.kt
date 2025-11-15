package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId

class InMemoryTilkommenInntektRepository : TilkommenInntektRepository {
    override fun finnAlleForFødselsnummer(fødselsnummer: String): List<TilkommenInntekt> {
        TODO("Not yet implemented")
    }

    override fun finn(id: TilkommenInntektId): TilkommenInntekt? {
        TODO("Not yet implemented")
    }

    override fun lagre(tilkommenInntekt: TilkommenInntekt) {
        TODO("Not yet implemented")
    }
}
