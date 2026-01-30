package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle.SELVSTSTENDIG_NÆRINGSDRIVENDE_BETA
import java.util.UUID

class TilgangsgrupperTilBrukerroller(
    val næringsdrivendeBeta: List<UUID>,
    val beslutter: List<UUID>,
    val egenAnsatt: List<UUID>,
    val kode7: List<UUID>,
    val stikkprøve: List<UUID>,
) {
    fun finnBrukerrollerFraTilgangsgrupper(tilgangsgrupper: Collection<UUID>): Set<Brukerrolle> {
        val roller = mutableSetOf<Brukerrolle>()
        if (tilgangsgrupper.any { næringsdrivendeBeta.contains(it) }) {
            roller.add(SELVSTSTENDIG_NÆRINGSDRIVENDE_BETA)
        }
        if (tilgangsgrupper.any { beslutter.contains(it) }) {
            roller.add(Brukerrolle.BESLUTTER)
        }
        if (tilgangsgrupper.any { egenAnsatt.contains(it) }) {
            roller.add(Brukerrolle.EGEN_ANSATT)
        }
        if (tilgangsgrupper.any { kode7.contains(it) }) {
            roller.add(Brukerrolle.KODE_7)
        }
        if (tilgangsgrupper.any { stikkprøve.contains(it) }) {
            roller.add(Brukerrolle.STIKKPRØVE)
        }
        return roller
    }
}
