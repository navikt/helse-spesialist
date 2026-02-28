package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.util.UUID

class TilgangsgrupperTilTilganger(
    val skrivetilgang: List<UUID>,
    val lesetilgang: List<UUID>,
) {
    fun finnTilgangerFraTilgangsgrupper(tilgangsgrupper: Collection<UUID>): Set<Tilgang> {
        val tilganger = mutableSetOf<Tilgang>()
        if (tilgangsgrupper.any { it in skrivetilgang }) {
            tilganger.add(Tilgang.Skriv)
            tilganger.add(Tilgang.Les)
        }
        if (tilgangsgrupper.any { it in lesetilgang }) {
            tilganger.add(Tilgang.Les)
        }
        return tilganger
    }
}
