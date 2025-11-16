package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId

class InMemoryTilkommenInntektRepository : TilkommenInntektRepository,
    AbstractInMemoryRepository<TilkommenInntektId, TilkommenInntekt>() {
    override fun finnAlleForFødselsnummer(fødselsnummer: String): List<TilkommenInntekt> =
        alle().filter { it.fødselsnummer == fødselsnummer }

    override fun deepCopy(original: TilkommenInntekt): TilkommenInntekt =
        TilkommenInntekt.fraLagring(original.events)
}
