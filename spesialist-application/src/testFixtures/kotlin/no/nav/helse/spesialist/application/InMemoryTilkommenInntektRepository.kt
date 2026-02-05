package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId

class InMemoryTilkommenInntektRepository : TilkommenInntektRepository,
    AbstractInMemoryRepository<TilkommenInntektId, TilkommenInntekt>() {
    override fun finnAlleForIdentitetsnummer(identitetsnummer: Identitetsnummer): List<TilkommenInntekt> =
        alle().filter { it.identitetsnummer == identitetsnummer }

    override fun deepCopy(original: TilkommenInntekt): TilkommenInntekt =
        TilkommenInntekt.fraLagring(original.events)
}
