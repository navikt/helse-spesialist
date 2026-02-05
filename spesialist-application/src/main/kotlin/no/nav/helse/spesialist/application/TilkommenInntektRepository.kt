package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId

interface TilkommenInntektRepository {
    fun finnAlleForIdentitetsnummer(identitetsnummer: Identitetsnummer): List<TilkommenInntekt>

    fun finn(id: TilkommenInntektId): TilkommenInntekt?

    fun lagre(tilkommenInntekt: TilkommenInntekt)
}
