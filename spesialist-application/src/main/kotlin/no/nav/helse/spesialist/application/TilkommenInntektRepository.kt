package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.gradering.TilkommenInntekt
import no.nav.helse.spesialist.domain.gradering.TilkommenInntektId

interface TilkommenInntektRepository {
    fun finnAlleForFødselsnummer(fødselsnummer: String): List<TilkommenInntekt>

    fun finn(id: TilkommenInntektId): TilkommenInntekt?

    fun lagre(tilkommenInntekt: TilkommenInntekt)
}
