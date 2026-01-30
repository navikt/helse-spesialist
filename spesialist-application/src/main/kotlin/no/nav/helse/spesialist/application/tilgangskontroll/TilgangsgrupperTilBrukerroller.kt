package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle.SELVSTSTENDIG_NÆRINGSDRIVENDE_BETA
import java.util.UUID

class TilgangsgrupperTilBrukerroller(
    val næringsdrivendeBeta: List<UUID>,
) {
    fun finnBrukerrollerFraTilgangsgrupper(tilgangsgrupper: Collection<UUID>): Set<Brukerrolle> {
        val roller = mutableSetOf<Brukerrolle>()
        if (tilgangsgrupper.any { næringsdrivendeBeta.contains(it) }) {
            roller.add(SELVSTSTENDIG_NÆRINGSDRIVENDE_BETA)
        }
        return roller
    }
}
