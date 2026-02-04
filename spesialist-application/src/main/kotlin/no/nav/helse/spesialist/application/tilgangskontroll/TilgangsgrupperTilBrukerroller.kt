package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle.SelvstendigNæringsdrivendeBeta
import java.util.UUID

class TilgangsgrupperTilBrukerroller(
    val næringsdrivendeBeta: List<UUID>,
    val beslutter: List<UUID>,
    val egenAnsatt: List<UUID>,
    val kode7: List<UUID>,
    val stikkprøve: List<UUID>,
    val feilsøking: List<UUID>,
) {
    fun finnBrukerrollerFraTilgangsgrupper(tilgangsgrupper: Collection<UUID>): Set<Brukerrolle> {
        val roller = mutableSetOf<Brukerrolle>()
        if (tilgangsgrupper.any { it in næringsdrivendeBeta }) {
            roller.add(SelvstendigNæringsdrivendeBeta)
        }
        if (tilgangsgrupper.any { it in beslutter }) {
            roller.add(Brukerrolle.Beslutter)
        }
        if (tilgangsgrupper.any { it in egenAnsatt }) {
            roller.add(Brukerrolle.EgenAnsatt)
        }
        if (tilgangsgrupper.any { it in kode7 }) {
            roller.add(Brukerrolle.Kode7)
        }
        if (tilgangsgrupper.any { it in stikkprøve }) {
            roller.add(Brukerrolle.Stikkprøve)
        }
        if (tilgangsgrupper.any { it in feilsøking }) {
            roller.add(Brukerrolle.Feilsøking)
        }
        return roller
    }

    fun alleUuider(): Set<UUID> =
        næringsdrivendeBeta.toSet() +
            beslutter.toSet() +
            egenAnsatt.toSet() +
            kode7.toSet() +
            stikkprøve.toSet() +
            feilsøking.toSet()
}
